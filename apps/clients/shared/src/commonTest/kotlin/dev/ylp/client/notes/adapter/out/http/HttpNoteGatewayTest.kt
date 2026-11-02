package dev.ylp.client.notes.adapter.out.http

import dev.ylp.client.generated.apis.NotesApi
import dev.ylp.client.notes.domain.DeleteNoteResult
import dev.ylp.client.notes.domain.Note
import dev.ylp.client.notes.domain.NoteDraft
import dev.ylp.client.notes.domain.NoteFailure
import dev.ylp.client.notes.domain.NotePage
import dev.ylp.client.notes.domain.NotePageResult
import dev.ylp.client.notes.domain.NoteResult
import dev.ylp.client.notes.domain.Violation
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** The API's answers, as the use cases read them. */
class HttpNoteGatewayTest {
    private val requests = mutableListOf<HttpRequestData>()

    private fun gateway(answer: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): HttpNoteGateway {
        val json = Json { ignoreUnknownKeys = true }
        val http =
            HttpClient(
                MockEngine { request ->
                    requests += request
                    answer(request)
                },
            ) {
                expectSuccess = false
                install(ContentNegotiation) {
                    json(json)
                    json(json, ContentType.Application.ProblemJson)
                }
            }
        return HttpNoteGateway(NotesApi("https://api.example/api/v1", http))
    }

    @Test
    fun `a page is read with its items`() =
        runTest {
            val result = gateway { json(PAGE) }.list(1, 5)

            assertEquals(NotePageResult.Success(NotePage(listOf(NOTE), page = 1, size = 5, totalNotes = 6, hasNext = false)), result)
            assertEquals("https://api.example/api/v1/notes?page=1&size=5", requests.single().url.toString())
        }

    @Test
    fun `a new note is sent as title and content`() =
        runTest {
            assertEquals(NoteResult.Success(NOTE), gateway { json(NOTE_JSON) }.create(NoteDraft("Groceries", "Milk")))

            assertEquals(
                """{"title":"Groceries","content":"Milk"}""",
                requests
                    .single()
                    .body
                    .toByteArray()
                    .decodeToString(),
            )
        }

    @Test
    fun `a change and a deletion name the version they are based on`() =
        runTest {
            val gateway = gateway { if (it.method.value == "DELETE") respond("", HttpStatusCode.NoContent) else json(NOTE_JSON) }

            gateway.update(NOTE.id, NoteDraft("Groceries", "Milk"), version = 4)
            assertEquals(DeleteNoteResult.Deleted, gateway.delete(NOTE.id, version = 4))

            assertEquals(listOf("\"4\"", "\"4\""), requests.map { it.headers[HttpHeaders.IfMatch] })
        }

    @Test
    fun `someone else's note is not found`() =
        runTest {
            assertEquals(
                NoteResult.Failed(NoteFailure.NotFound),
                gateway { problem(HttpStatusCode.NotFound, "Note not found") }.get(NOTE.id),
            )
        }

    @Test
    fun `a note changed since the version a change is based on reports the change`() =
        runTest {
            val gateway = gateway { problem(HttpStatusCode.PreconditionFailed, "Note is at version 5, not 4") }

            assertEquals(NoteResult.Failed(NoteFailure.Changed), gateway.update(NOTE.id, NoteDraft("Groceries", "Milk"), 4))
            assertEquals(DeleteNoteResult.Failed(NoteFailure.Changed), gateway.delete(NOTE.id, 4))
        }

    @Test
    fun `a request the API rejects reports its detail`() =
        runTest {
            val result = gateway { problem(HttpStatusCode.BadRequest, "title: must not be blank") }.create(NoteDraft(" ", "Milk"))

            assertEquals(NoteResult.Failed(NoteFailure.Invalid(listOf(Violation(null, "title: must not be blank")))), result)
        }

    @Test
    fun `a rejection without a problem body still reports the request as invalid`() =
        runTest {
            val result = gateway { respond("", HttpStatusCode.PayloadTooLarge) }.update(NOTE.id, NoteDraft("Groceries", "Milk"), 0)

            assertEquals(NoteResult.Failed(NoteFailure.Invalid(listOf(Violation(null, "Invalid request")))), result)
        }

    @Test
    fun `a problem without detail reports its title`() =
        runTest {
            val result =
                gateway {
                    respond(
                        """{"type": "about:blank", "title": "Bad Request", "status": 400}""",
                        HttpStatusCode.BadRequest,
                        headersOf(HttpHeaders.ContentType, "application/problem+json"),
                    )
                }.create(NoteDraft("Groceries", "Milk"))

            assertEquals(NoteResult.Failed(NoteFailure.Invalid(listOf(Violation(null, "Bad Request")))), result)
        }

    @Test
    fun `an unexpected answer without a problem body reports its status`() =
        runTest {
            assertEquals(
                NotePageResult.Failed(NoteFailure.Unavailable("Unexpected answer from the API: 502")),
                gateway { respond("", HttpStatusCode.BadGateway) }.list(0, 20),
            )
        }

    @Test
    fun `an unexpected error of the API is reported as unavailable`() =
        runTest {
            assertEquals(
                DeleteNoteResult.Failed(NoteFailure.Unavailable("Internal Server Error")),
                gateway { problem(HttpStatusCode.InternalServerError, "boom") }.delete(NOTE.id, 0),
            )
        }

    @Test
    fun `an unreachable API is reported as unavailable`() =
        runTest {
            assertEquals(
                NoteResult.Failed(NoteFailure.Unavailable("offline")),
                gateway { throw IllegalStateException("offline") }.get(NOTE.id),
            )
        }

    @Test
    fun `a call without a valid token is signed out`() =
        runTest {
            assertEquals(NoteResult.Failed(NoteFailure.SignedOut), gateway { respond("", HttpStatusCode.Unauthorized) }.get(NOTE.id))
        }

    private fun MockRequestHandleScope.problem(
        status: HttpStatusCode,
        detail: String,
    ) = respond(
        """{"type": "about:blank", "title": "${status.description}", "status": ${status.value}, "detail": "$detail"}""",
        status,
        headersOf(HttpHeaders.ContentType, "application/problem+json"),
    )

    private fun MockRequestHandleScope.json(body: String) =
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

    private companion object {
        val NOTE =
            Note(
                id = "3f0c1e6a-8b2d-4f7e-9a1c-5d6e7f8a9b0c",
                title = "Groceries",
                content = "Milk",
                createdAt = Instant.parse("2026-10-22T08:00:00Z"),
                updatedAt = Instant.parse("2026-10-22T09:30:00Z"),
                version = 4,
            )
        const val NOTE_JSON =
            """{"id": "3f0c1e6a-8b2d-4f7e-9a1c-5d6e7f8a9b0c", "title": "Groceries", "content": "Milk",
                "createdAt": "2026-10-22T08:00:00Z", "updatedAt": "2026-10-22T09:30:00Z", "version": 4}"""
        const val PAGE = """{"items": [$NOTE_JSON], "page": 1, "size": 5, "totalItems": 6, "hasNext": false}"""
    }
}
