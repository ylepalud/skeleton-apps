package dev.ylp.client.auth.application

import dev.ylp.client.auth.domain.TokenStore

/** Tells whether a user is signed in on this device. An expired access token is refreshed on use. */
class CurrentSession internal constructor(
    private val tokens: TokenStore,
) {
    suspend fun execute(): Session = if (tokens.load() == null) Session.SignedOut else Session.SignedIn
}

sealed class Session {
    data object SignedIn : Session()

    data object SignedOut : Session()
}
