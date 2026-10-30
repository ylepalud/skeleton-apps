package dev.ylp.client

import dev.ylp.client.auth.adapter.out.memory.InMemoryTokenStore
import dev.ylp.client.auth.application.Session
import dev.ylp.client.auth.domain.AuthConfig
import dev.ylp.client.auth.domain.AuthorizationBrowser
import dev.ylp.client.auth.domain.TokenStore
import dev.ylp.client.auth.domain.Tokens
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** A port implemented the way the iOS app does it: a subclass of [PlatformAdapter]. */
class PlatformAdapterTest {
    private class Store :
        PlatformAdapter(),
        TokenStore by InMemoryTokenStore()

    private class Browser :
        PlatformAdapter(),
        AuthorizationBrowser {
        override suspend fun authorize(
            url: String,
            redirectUri: String,
        ): String? = null
    }

    @Test
    fun `a platform adapter serves as a port of the client`() =
        runTest {
            val store = Store()
            val client =
                SharedClient(
                    "https://api.example/api/v1",
                    AuthConfig("https://idp.example/default", "app", "app://callback"),
                    Browser(),
                    store,
                )

            store.save(Tokens("access", null, null))

            assertEquals(Session.SignedIn, client.currentSession.execute())
        }
}
