package dev.ylp.client.auth.application

import dev.ylp.client.auth.adapter.out.memory.InMemoryTokenStore
import dev.ylp.client.auth.domain.AuthenticationException
import dev.ylp.client.auth.domain.Authorization
import dev.ylp.client.auth.domain.DeviceAuthorization
import dev.ylp.client.auth.domain.DeviceCodeDisplay
import dev.ylp.client.auth.domain.DevicePoll
import dev.ylp.client.auth.domain.IdentityProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// currentTime reads the virtual clock: the polling waits without slowing the tests.
@OptIn(ExperimentalCoroutinesApi::class)
class SignInOnDeviceTest {
    private val store = InMemoryTokenStore()
    private val deviceData = FakeDeviceData()
    private val shown = mutableListOf<DeviceAuthorization>()
    private val display =
        object : DeviceCodeDisplay {
            override suspend fun show(authorization: DeviceAuthorization) {
                shown += authorization
            }
        }

    private fun signIn(vararg polls: DevicePoll) = FakeIdentityProvider(polls = polls.toList())

    @Test
    fun `the code is shown then the tokens kept once the user signs in elsewhere`() =
        runTest {
            val provider = signIn(DevicePoll.Pending, DevicePoll.Granted(Authorization(FakeIdentityProvider.TOKENS, "alice")))

            val result = SignInOnDevice(provider, store, deviceData).execute(display)

            assertEquals(DeviceSignInResult.SignedIn, result)
            assertEquals(listOf(FakeIdentityProvider.DEVICE), shown)
            assertEquals(FakeIdentityProvider.TOKENS, store.load())
            assertEquals(listOf("claim alice"), deviceData.calls)
            assertEquals(10_000, currentTime)
        }

    @Test
    fun `a provider asking to slow down is polled 5 seconds less often`() =
        runTest {
            val provider = signIn(DevicePoll.SlowDown, DevicePoll.Granted(Authorization(FakeIdentityProvider.TOKENS, null)))

            SignInOnDevice(provider, store, deviceData).execute(display)

            assertEquals(5_000 + 10_000, currentTime)
        }

    @Test
    fun `a refusal or an expired code ends the sign-in`() =
        runTest {
            assertEquals(
                DeviceSignInResult.Failed("The sign-in was refused"),
                SignInOnDevice(signIn(DevicePoll.Denied), store, deviceData).execute(display),
            )
            assertEquals(
                DeviceSignInResult.Failed("The code expired before the sign-in"),
                SignInOnDevice(signIn(DevicePoll.Expired), store, deviceData).execute(display),
            )
            assertNull(store.load())
        }

    @Test
    fun `a provider without the device flow says so before showing anything`() =
        runTest {
            assertEquals(DeviceSignInResult.Unsupported, SignInOnDevice(FakeIdentityProvider(), store, deviceData).execute(display))
            assertEquals(emptyList(), shown)
        }

    @Test
    fun `a provider failure while waiting fails the sign-in with its message`() =
        runTest {
            val failing =
                object : IdentityProvider by signIn() {
                    override suspend fun pollDeviceToken(authorization: DeviceAuthorization): DevicePoll =
                        throw AuthenticationException("invalid_client")
                }

            assertEquals(DeviceSignInResult.Failed("invalid_client"), SignInOnDevice(failing, store, deviceData).execute(display))
        }

    @Test
    fun `the device code never shows in logs`() {
        assertEquals("DeviceAuthorization(userCode=WDJB-MJHT, verificationUri=https://idp/device)", FakeIdentityProvider.DEVICE.toString())
    }
}
