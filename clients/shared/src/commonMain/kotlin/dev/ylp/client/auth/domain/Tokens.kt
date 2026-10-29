package dev.ylp.client.auth.domain

import kotlin.time.Instant

/** The tokens of a signed-in user. Never printed: [toString] hides them. */
data class Tokens(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAt: Instant?,
) {
    override fun toString(): String = "Tokens(expiresAt=$expiresAt, refreshable=${refreshToken != null})"
}
