package dev.ylp.client.auth.application

import dev.ylp.client.auth.domain.AuthenticationException
import dev.ylp.client.auth.domain.DeviceCodeDisplay
import dev.ylp.client.auth.domain.DeviceData
import dev.ylp.client.auth.domain.DevicePoll
import dev.ylp.client.auth.domain.IdentityProvider
import dev.ylp.client.auth.domain.TokenStore
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/**
 * Signs the user in with the device flow (RFC 8628), for a client without a browser of its own:
 * [display] shows a code the user types on another device, while this waits for the provider.
 */
class SignInOnDevice internal constructor(
    private val identityProvider: IdentityProvider,
    private val tokens: TokenStore,
    private val deviceData: DeviceData,
) {
    suspend fun execute(display: DeviceCodeDisplay): DeviceSignInResult =
        try {
            val authorization = identityProvider.startDeviceAuthorization()
            if (authorization == null) {
                DeviceSignInResult.Unsupported
            } else {
                display.show(authorization)
                var interval = authorization.intervalSeconds.seconds
                var result: DeviceSignInResult? = null
                while (result == null) {
                    delay(interval)
                    when (val poll = identityProvider.pollDeviceToken(authorization)) {
                        // The user has not finished: wait another interval.
                        DevicePoll.Pending -> {
                            continue
                        }

                        // RFC 8628: each slow_down adds 5 seconds to the interval.
                        DevicePoll.SlowDown -> {
                            interval += SLOW_DOWN
                        }

                        is DevicePoll.Granted -> {
                            deviceData.claimFor(poll.authorization.subject)
                            tokens.save(poll.authorization.tokens)
                            result = DeviceSignInResult.SignedIn
                        }

                        DevicePoll.Denied -> {
                            result = DeviceSignInResult.Failed("The sign-in was refused")
                        }

                        DevicePoll.Expired -> {
                            result = DeviceSignInResult.Failed("The code expired before the sign-in")
                        }
                    }
                }
                result
            }
        } catch (e: AuthenticationException) {
            DeviceSignInResult.Failed(e.message)
        }

    private companion object {
        val SLOW_DOWN = 5.seconds
    }
}

/** The outcome of a sign-in with the device flow. */
sealed class DeviceSignInResult {
    data object SignedIn : DeviceSignInResult()

    /** The provider does not offer the device flow: sign in with [SignIn] instead. */
    data object Unsupported : DeviceSignInResult()

    data class Failed(
        val message: String,
    ) : DeviceSignInResult()
}
