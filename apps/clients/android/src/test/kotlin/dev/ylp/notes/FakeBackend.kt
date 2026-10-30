package dev.ylp.notes

import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * The API and the identity provider on one mock server, with notes kept in memory. It answers
 * as the real API does: pages, 404 for an unknown note, 401 for an unknown token.
 */
class FakeBackend : Dispatcher() {
    val server = MockWebServer()
    val notes = linkedMapOf<String, Pair<String, String>>()
    val requests = mutableListOf<String>()

    /** When false, the token endpoint refuses every refresh: the session is over. */
    var refreshAllowed = true

    /** When set, the API answers this status to every call once, then behaves again. */
    var failNextWith: Int? = null

    fun start() {
        server.dispatcher = this
        server.start()
    }

    fun url(path: String): String = server.url(path).toString().trimEnd('/')

    fun add(
        title: String,
        content: String = "",
    ): String = UUID.randomUUID().toString().also { notes[it] = title to content }

    override fun dispatch(request: RecordedRequest): MockResponse {
        val path = request.url.encodedPath
        val method = request.method
        requests += "$method $path"
        return when {
            path == "/idp/.well-known/openid-configuration" ->
                json(200, JSONObject().put("authorization_endpoint", url("/idp/authorize")).put("token_endpoint", url("/idp/token")))

            path == "/idp/token" ->
                if (request.body?.utf8()?.contains("grant_type=refresh_token") == true && !refreshAllowed) {
                    json(400, JSONObject().put("error", "invalid_grant"))
                } else {
                    json(200, JSONObject().put("access_token", VALID_TOKEN).put("refresh_token", "refresh").put("expires_in", 300))
                }

            request.headers["Authorization"] != "Bearer $VALID_TOKEN" -> MockResponse.Builder().code(401).build()

            failNextWith != null -> MockResponse.Builder().code(failNextWith!!).build().also { failNextWith = null }

            else -> api(method, path.removePrefix("/api/v1"), request)
        }
    }

    private fun api(
        method: String?,
        path: String,
        request: RecordedRequest,
    ): MockResponse {
        val id = path.removePrefix("/notes/").takeIf { path.startsWith("/notes/") }
        return when {
            path == "/notes" && method == "GET" -> {
                val page = request.url.queryParameter("page")?.toInt() ?: 0
                val size = request.url.queryParameter("size")?.toInt() ?: 20
                val items = notes.entries.toList().drop(page * size).take(size)
                val body =
                    JSONObject()
                        .put("items", JSONArray(items.map { note(it.key) }))
                        .put("page", page)
                        .put("size", size)
                        .put("totalItems", notes.size)
                        .put("hasNext", (page + 1) * size < notes.size)
                json(200, body)
            }

            path == "/notes" && method == "POST" -> {
                val body = JSONObject(request.body!!.utf8())
                json(201, note(add(body.getString("title"), body.getString("content"))))
            }

            id == null || id !in notes -> json(404, JSONObject().put("type", "about:blank").put("title", "Not Found").put("status", 404))

            method == "GET" -> json(200, note(id))

            method == "PUT" -> {
                val body = JSONObject(request.body!!.utf8())
                notes[id] = body.getString("title") to body.getString("content")
                json(200, note(id))
            }

            method == "DELETE" -> MockResponse.Builder().code(204).build().also { notes.remove(id) }

            else -> MockResponse.Builder().code(405).build()
        }
    }

    private fun note(id: String): JSONObject {
        val (title, content) = notes.getValue(id)
        return JSONObject()
            .put("id", id)
            .put("title", title)
            .put("content", content)
            .put("createdAt", "2026-10-29T08:00:00Z")
            .put("updatedAt", "2026-10-29T09:00:00Z")
    }

    private fun json(
        status: Int,
        body: JSONObject,
    ): MockResponse {
        val type = if (status >= 400) "application/problem+json" else "application/json"
        return MockResponse.Builder().code(status).addHeader("Content-Type", type).body(body.toString()).build()
    }

    companion object {
        const val VALID_TOKEN = "valid-token"
    }
}
