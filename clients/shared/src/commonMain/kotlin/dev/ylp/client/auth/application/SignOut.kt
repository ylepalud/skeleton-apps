package dev.ylp.client.auth.application

import dev.ylp.client.auth.domain.TokenStore

/** Forgets the user's tokens on this device. */
class SignOut internal constructor(
    private val tokens: TokenStore,
) {
    suspend fun execute() = tokens.clear()
}
