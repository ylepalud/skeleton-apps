package dev.ylp.client.auth.domain

/**
 * A device authorization in progress (RFC 8628): the user opens [verificationUri] on any device
 * and types [userCode], while the client waits. [deviceCode] is the client's secret: [toString]
 * hides it.
 */
data class DeviceAuthorization(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    /** The same page with the code filled in, when the provider offers one. */
    val verificationUriComplete: String?,
    /** How long to wait between two polls, in seconds. */
    val intervalSeconds: Long,
) {
    override fun toString(): String = "DeviceAuthorization(userCode=$userCode, verificationUri=$verificationUri)"
}

/** What the provider answers while the client waits for the user. */
sealed class DevicePoll {
    /** The user has not finished yet. */
    data object Pending : DevicePoll()

    /** The client polls too often: it waits longer. */
    data object SlowDown : DevicePoll()

    data class Granted(
        val authorization: Authorization,
    ) : DevicePoll()

    /** The user refused the sign-in. */
    data object Denied : DevicePoll()

    /** The code expired before the user signed in. */
    data object Expired : DevicePoll()
}

/** Shows the user where to sign in and the code to type there. Each client provides it. */
interface DeviceCodeDisplay {
    suspend fun show(authorization: DeviceAuthorization)
}
