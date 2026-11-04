package dev.ylp.client.auth.application

import dev.ylp.client.auth.domain.AuthenticationException
import dev.ylp.client.auth.domain.Authorization
import dev.ylp.client.auth.domain.AuthorizationBrowser
import dev.ylp.client.auth.domain.AuthorizationRequest
import dev.ylp.client.auth.domain.DeviceAuthorization
import dev.ylp.client.auth.domain.DevicePoll
import dev.ylp.client.auth.domain.IdentityProvider
import dev.ylp.client.auth.domain.Tokens

/** A browser that answers with a fixed redirect, or null as if the user cancelled. */
class FakeBrowser(
    private val redirect: String?,
) : AuthorizationBrowser {
    var opened: String? = null

    override suspend fun authorize(
        url: String,
        redirectUri: String,
    ): String? {
        opened = url
        return redirect
    }
}

/**
 * A provider that issues [TOKENS] to [subject], or refuses with [failure] when it is set. Its
 * device flow answers [polls] in turn, or is missing when [polls] is null.
 */
class FakeIdentityProvider(
    private val failure: String? = null,
    private val subject: String? = "alice",
    polls: List<DevicePoll>? = null,
) : IdentityProvider {
    private val polls = polls?.toMutableList()
    var pollCount = 0

    override suspend fun startAuthorization() = AuthorizationRequest("https://idp/authorize", "state", "verifier")

    override suspend fun completeAuthorization(
        request: AuthorizationRequest,
        redirectUrl: String,
    ): Authorization = failure?.let { throw AuthenticationException(it) } ?: Authorization(TOKENS, subject)

    override suspend fun refresh(refreshToken: String): Tokens = TOKENS

    override suspend fun startDeviceAuthorization(): DeviceAuthorization? = polls?.let { DEVICE }

    override suspend fun pollDeviceToken(authorization: DeviceAuthorization): DevicePoll {
        pollCount++
        return polls!!.removeAt(0)
    }

    companion object {
        val TOKENS = Tokens("access", "refresh", null)
        val DEVICE = DeviceAuthorization("device-secret", "WDJB-MJHT", "https://idp/device", null, intervalSeconds = 5)
    }
}

/** The data of the device, as calls. */
class FakeDeviceData : dev.ylp.client.auth.domain.DeviceData {
    val calls = mutableListOf<String>()

    override suspend fun claimFor(subject: String?) {
        calls += "claim $subject"
    }

    override suspend fun clear() {
        calls += "clear"
    }
}
