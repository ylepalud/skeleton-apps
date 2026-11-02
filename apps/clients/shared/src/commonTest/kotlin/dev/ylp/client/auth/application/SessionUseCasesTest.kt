package dev.ylp.client.auth.application

import dev.ylp.client.auth.adapter.out.memory.InMemoryTokenStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionUseCasesTest {
    private val store = InMemoryTokenStore()
    private val deviceData = FakeDeviceData()

    @Test
    fun `signing in opens the provider's page and keeps the tokens`() =
        runTest {
            val browser = FakeBrowser("app://callback?code=c&state=state")

            val result = SignIn(FakeIdentityProvider(), browser, store, deviceData, "app://callback").execute()

            assertEquals(SignInResult.SignedIn, result)
            assertEquals("https://idp/authorize", browser.opened)
            assertEquals(FakeIdentityProvider.TOKENS, store.load())
            assertEquals(Session.SignedIn, CurrentSession(store).execute())
            assertEquals(listOf("claim alice"), deviceData.calls)
        }

    @Test
    fun `closing the browser cancels the sign-in`() =
        runTest {
            val result = SignIn(FakeIdentityProvider(), FakeBrowser(null), store, deviceData, "app://callback").execute()

            assertEquals(SignInResult.Cancelled, result)
            assertNull(store.load())
        }

    @Test
    fun `a refusal of the provider fails the sign-in`() =
        runTest {
            val result =
                SignIn(
                    FakeIdentityProvider("access_denied"),
                    FakeBrowser("app://callback"),
                    store,
                    deviceData,
                    "app://callback",
                ).execute()

            assertEquals(SignInResult.Failed("access_denied"), result)
            assertEquals(Session.SignedOut, CurrentSession(store).execute())
        }

    @Test
    fun `signing out forgets the tokens`() =
        runTest {
            store.save(FakeIdentityProvider.TOKENS)

            SignOut(store, deviceData).execute()

            assertEquals(Session.SignedOut, CurrentSession(store).execute())
            assertEquals(listOf("clear"), deviceData.calls)
        }
}
