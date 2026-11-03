package dev.ylp.notes.cli

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.header
import io.ktor.server.request.receiveParameters
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The API and the identity provider on one local server, with notes kept in memory, answering as
 * the real ones do: versions, 404 for an unknown note, 401 without a valid token. The provider
 * offers the device flow when [deviceFlow] is true; [offline] makes the API answer 503.
 */
class FakeBackend {
    data class StoredNote(
        val title: String,
        val content: String,
        val version: Long,
    )

    val notes = linkedMapOf<String, StoredNote>()
    var offline = false
    var deviceFlow = false

    /** The provider's answer to a token request that is not a refresh, when it refuses one. */
    var tokenError: String? = null
    var refreshAllowed = true
    private var lastId = 0
    private lateinit var server: EmbeddedServer<*, *>
    var port = 0
        private set

    fun url(path: String) = "http://127.0.0.1:$port$path"

    fun add(title: String): String = "n${++lastId}".also { notes[it] = StoredNote(title, "", 0) }

    fun changeElsewhere(
        id: String,
        title: String,
    ) {
        notes[id] = notes.getValue(id).let { it.copy(title = title, version = it.version + 1) }
    }

    suspend fun start() {
        server =
            embeddedServer(CIO, host = "127.0.0.1", port = 0) {
                routing {
                    identityProvider()
                    api()
                }
            }
        server.startSuspend(wait = false)
        port =
            server.engine
                .resolvedConnectors()
                .first()
                .port
    }

    suspend fun stop() = server.stopSuspend(0, 500)

    private fun io.ktor.server.routing.Routing.identityProvider() {
        get("/idp/.well-known/openid-configuration") {
            val device = if (deviceFlow) """, "device_authorization_endpoint": "${url("/idp/device")}"""" else ""
            json("""{"authorization_endpoint": "${url("/idp/authorize")}", "token_endpoint": "${url("/idp/token")}"$device}""")
        }
        get("/idp/authorize") {
            val parameters = call.request.queryParameters
            call.respondRedirect("${parameters["redirect_uri"]}?code=the-code&state=${parameters["state"]}")
        }
        post("/idp/device") {
            json(
                """{"device_code": "secret", "user_code": "WDJB-MJHT", "verification_uri": "${url("/idp/activate")}",
                    "verification_uri_complete": "${url("/idp/activate?user_code=WDJB-MJHT")}", "interval": 1}""",
            )
        }
        post("/idp/token") {
            val refreshing = call.receiveParameters()["grant_type"] == "refresh_token"
            val error = if (refreshing) "invalid_grant".takeUnless { refreshAllowed } else tokenError
            if (error != null) {
                json("""{"error": "$error"}""", HttpStatusCode.BadRequest)
            } else {
                json("""{"access_token": "$TOKEN", "refresh_token": "refresh", "expires_in": 3600}""")
            }
        }
    }

    private fun io.ktor.server.routing.Routing.api() {
        get("/api/v1/notes") {
            guarded {
                val page = call.request.queryParameters["page"]?.toInt() ?: 0
                val size = call.request.queryParameters["size"]?.toInt() ?: 20
                val ids =
                    notes.keys
                        .reversed()
                        .drop(page * size)
                        .take(size)
                val items = ids.joinToString(",") { note(it) }
                val hasNext = (page + 1) * size < notes.size
                json("""{"items": [$items], "page": $page, "size": $size, "totalItems": ${notes.size}, "hasNext": $hasNext}""")
            }
        }
        post("/api/v1/notes") {
            guarded {
                val body = Json.parseToJsonElement(call.receiveText()).jsonObject
                val id = "n${++lastId}"
                notes[id] = StoredNote(body.getValue("title").jsonPrimitive.content, body.getValue("content").jsonPrimitive.content, 0)
                json(note(id), HttpStatusCode.Created)
            }
        }
        get("/api/v1/notes/{id}") { guarded { withNote { id -> json(note(id)) } } }
        put("/api/v1/notes/{id}") {
            guarded {
                withVersion { id ->
                    val body = Json.parseToJsonElement(call.receiveText()).jsonObject
                    notes[id] =
                        StoredNote(
                            body.getValue("title").jsonPrimitive.content,
                            body.getValue("content").jsonPrimitive.content,
                            notes.getValue(id).version + 1,
                        )
                    json(note(id))
                }
            }
        }
        delete("/api/v1/notes/{id}") {
            guarded {
                withVersion { id ->
                    notes.remove(id)
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }

    private suspend fun RoutingContext.guarded(answer: suspend RoutingContext.() -> Unit) {
        when {
            offline -> call.respond(HttpStatusCode.ServiceUnavailable)
            call.request.header("Authorization") != "Bearer $TOKEN" -> call.respond(HttpStatusCode.Unauthorized)
            else -> answer()
        }
    }

    private suspend fun RoutingContext.withNote(answer: suspend RoutingContext.(String) -> Unit) {
        val id = call.parameters["id"].orEmpty()
        if (id !in notes) json("""{"title": "Not Found", "status": 404}""", HttpStatusCode.NotFound) else answer(id)
    }

    private suspend fun RoutingContext.withVersion(answer: suspend RoutingContext.(String) -> Unit) =
        withNote { id ->
            if (call.request.header("If-Match") != "\"${notes.getValue(id).version}\"") {
                json("""{"title": "Precondition Failed", "status": 412}""", HttpStatusCode.PreconditionFailed)
            } else {
                answer(id)
            }
        }

    private fun note(id: String): String {
        val note = notes.getValue(id)
        return """{"id": "$id", "title": "${note.title}", "content": "${note.content}", "createdAt": "2026-11-03T08:00:00Z",
            "updatedAt": "2026-11-03T08:${(id.drop(1).toInt() % 60).toString().padStart(2, '0')}:00Z", "version": ${note.version}}"""
    }

    private suspend fun RoutingContext.json(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = call.respondText(
        body,
        if (status.value >=
            400
        ) {
            ContentType("application", "problem+json")
        } else {
            ContentType.Application.Json
        },
        status,
    )

    companion object {
        const val TOKEN = "valid-token"
    }
}
