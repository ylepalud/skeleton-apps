package dev.ylp.notes

import dev.ylp.client.SharedClient
import dev.ylp.client.auth.adapter.out.memory.InMemoryTokenStore
import dev.ylp.client.auth.domain.AuthConfig
import dev.ylp.client.auth.domain.AuthorizationBrowser
import io.ktor.http.Url

/** The app wired to [backend], with a browser that signs in at once and tokens kept in memory. */
class TestNotesApplication : NotesApplication() {
    override fun createClient(): SharedClient = SharedClient(
        apiBaseUrl = backend.url("/api/v1"),
        auth = AuthConfig(backend.url("/idp"), "api-mobile", REDIRECT_URI),
        browser = signInBrowser,
        tokenStore = tokens,
    )

    companion object {
        const val REDIRECT_URI = "dev.ylp.notes://callback"

        lateinit var backend: FakeBackend
            private set
        var tokens = InMemoryTokenStore()
            private set

        /** Answers the sign-in page with a valid redirect, or null as if the user closed it. */
        var userCancels = false

        /** A started backend and a signed-out user, for the next test. */
        fun reset(): FakeBackend {
            backend = FakeBackend().apply { start() }
            tokens = InMemoryTokenStore()
            userCancels = false
            return backend
        }

        val signInBrowser =
            object : AuthorizationBrowser {
                override suspend fun authorize(
                    url: String,
                    redirectUri: String,
                ): String? = if (userCancels) null else "$redirectUri?code=the-code&state=${Url(url).parameters["state"]}"
            }
    }
}
