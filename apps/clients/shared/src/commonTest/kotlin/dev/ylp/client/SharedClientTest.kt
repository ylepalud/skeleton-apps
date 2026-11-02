package dev.ylp.client

import dev.ylp.client.auth.adapter.out.memory.InMemoryTokenStore
import dev.ylp.client.auth.application.FakeBrowser
import dev.ylp.client.auth.domain.AuthConfig
import dev.ylp.client.auth.domain.Tokens
import dev.ylp.client.notes.domain.DeleteNoteResult
import dev.ylp.client.notes.domain.Note
import dev.ylp.client.notes.domain.NoteFailure
import dev.ylp.client.notes.domain.NotePage
import dev.ylp.client.notes.domain.NotePageResult
import dev.ylp.client.notes.domain.NoteResult
import dev.ylp.client.notes.domain.SyncState
import dev.ylp.client.sync.domain.SyncResult
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

/** The client end to end, over a mock of the API and of the identity provider. */
class SharedClientTest {
    private val store = InMemoryTokenStore()
    private val localStore = testStore()
    private val requests = mutableListOf<HttpRequestData>()

    private fun client(api: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): SharedClient {
        val engine =
            MockEngine { request ->
                requests += request
                when (request.url.host) {
                    "idp.example" -> {
                        when (request.url.encodedPath) {
                            "/default/.well-known/openid-configuration" -> {
                                json(
                                    """{"authorization_endpoint": "https://idp.example/default/authorize", "token_endpoint": "https://idp.example/default/token"}""",
                                )
                            }

                            else -> {
                                refresh(request)
                            }
                        }
                    }

                    else -> {
                        api(request)
                    }
                }
            }
        return SharedClient(
            "https://api.example/api/v1/",
            AuthConfig("https://idp.example/default", "mobile", "app://callback"),
            FakeBrowser(null),
            localStore,
            store,
            engine,
        )
    }

    private var refreshAnswer: String? = """{"access_token": "renewed", "refresh_token": "refresh-2"}"""

    private fun MockRequestHandleScope.refresh(request: HttpRequestData): HttpResponseData =
        refreshAnswer?.let { json(it) } ?: json("""{"error": "invalid_grant"}""", HttpStatusCode.BadRequest)

    @Test
    fun `notes are listed with the user's token`() =
        runTest {
            store.save(Tokens("access", "refresh", null))
            val client = client { json(PAGE) }

            val result = client.listNotes.execute(page = 1, size = 5)

            assertEquals(NotePageResult.Success(NotePage(listOf(NOTE), page = 1, size = 5, totalNotes = 6, hasNext = false)), result)
            val request = requests.single()
            assertEquals("https://api.example/api/v1/notes?page=1&size=5", request.url.toString())
            assertEquals("Bearer access", request.headers[HttpHeaders.Authorization])
        }

    @Test
    fun `a note is written then read then replaced then deleted under its local id`() =
        runTest {
            store.save(Tokens("access", "refresh", null))
            val client =
                client { request ->
                    if (request.method ==
                        HttpMethod.Delete
                    ) {
                        respond("", HttpStatusCode.NoContent)
                    } else {
                        json(NOTE_JSON)
                    }
                }

            val created = (client.createNote.execute("Groceries", "Milk") as NoteResult.Success).note
            assertEquals(NOTE.copy(id = created.id), created)
            assertEquals(NoteResult.Success(created), client.getNote.execute(created.id))
            assertEquals(NoteResult.Success(created), client.updateNote.execute(created.id, "Groceries", "Milk"))
            assertEquals(DeleteNoteResult.Deleted, client.deleteNote.execute(created.id))

            assertEquals(listOf(HttpMethod.Post, HttpMethod.Get, HttpMethod.Put, HttpMethod.Delete), requests.map { it.method })
            assertEquals("https://api.example/api/v1/notes/${NOTE.id}", requests[1].url.toString())
            assertEquals(listOf(null, null, "\"4\"", "\"4\""), requests.map { it.headers[HttpHeaders.IfMatch] })
            assertEquals(0, client.countPendingChanges.execute())
        }

    @Test
    fun `an unreachable API leaves the change queued until a sync gets through`() =
        runTest {
            store.save(Tokens("access", "refresh", null))
            var online = false
            val client = client { if (online) json(NOTE_JSON) else throw IllegalStateException("offline") }

            val created = (client.createNote.execute("Groceries", "Milk") as NoteResult.Success).note
            assertEquals(SyncState.PENDING, created.sync)
            assertEquals(SyncResult.OFFLINE, client.syncChanges.execute())

            online = true
            assertEquals(SyncResult.SYNCED, client.syncChanges.execute())
            assertEquals(0, client.countPendingChanges.execute())
        }

    @Test
    fun `signing out forgets the notes kept on the device`() =
        runTest {
            store.save(Tokens("access", "refresh", null))
            val client = client { throw IllegalStateException("offline") }
            client.createNote.execute("Groceries", "Milk")

            client.signOut.execute()

            assertEquals(0, client.countPendingChanges.execute())
            assertEquals(emptyList(), (client.listNotes.execute() as NotePageResult.Success).page.notes)
        }

    @Test
    fun `an expired token is refreshed and the request retried`() =
        runTest {
            store.save(Tokens("expired", "refresh-1", null))
            val client =
                client { request ->
                    if (request.headers[HttpHeaders.Authorization] ==
                        "Bearer renewed"
                    ) {
                        json(NOTE_JSON)
                    } else {
                        unauthorized()
                    }
                }

            assertEquals(NoteResult.Success(NOTE), client.getNote.execute(NOTE.id))
            assertEquals(Tokens("renewed", "refresh-2", null), store.load())
        }

    @Test
    fun `a refused refresh ends the session`() =
        runTest {
            refreshAnswer = null
            store.save(Tokens("expired", "refresh-1", null))
            val client = client { unauthorized() }

            assertEquals(NoteResult.Failed(NoteFailure.SignedOut), client.getNote.execute(NOTE.id))
            assertNull(store.load())
        }

    @Test
    fun `an expired token without a refresh token ends the session`() =
        runTest {
            store.save(Tokens("expired", null, null))
            val client = client { unauthorized() }

            assertEquals(NoteResult.Failed(NoteFailure.SignedOut), client.getNote.execute(NOTE.id))
            assertEquals(1, requests.size)
        }

    @Test
    fun `a client builds with the platform's own HTTP engine`() =
        runTest {
            val client =
                SharedClient(
                    "https://api.example/api/v1",
                    AuthConfig("https://idp.example/default", "mobile", "app://callback"),
                    FakeBrowser(null),
                    testStore(),
                )

            assertEquals(dev.ylp.client.auth.application.Session.SignedOut, client.currentSession.execute())
        }

    @Test
    fun `a call without a session is signed out`() =
        runTest {
            val client = client { unauthorized() }

            assertEquals(NotePageResult.Failed(NoteFailure.SignedOut), client.listNotes.execute())
        }

    private fun MockRequestHandleScope.unauthorized() =
        respond("", HttpStatusCode.Unauthorized, headersOf(HttpHeaders.WWWAuthenticate, "Bearer"))

    private fun MockRequestHandleScope.json(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

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
