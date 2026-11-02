package dev.ylp.client.auth.application

import dev.ylp.client.auth.domain.DeviceData
import dev.ylp.client.auth.domain.TokenStore

/**
 * Forgets the user's tokens and data on this device, changes not sent yet included: an app asks
 * first when [CountPendingChanges][dev.ylp.client.sync.application.CountPendingChanges] is not 0.
 */
class SignOut internal constructor(
    private val tokens: TokenStore,
    private val deviceData: DeviceData,
) {
    suspend fun execute() {
        tokens.clear()
        deviceData.clear()
    }
}
