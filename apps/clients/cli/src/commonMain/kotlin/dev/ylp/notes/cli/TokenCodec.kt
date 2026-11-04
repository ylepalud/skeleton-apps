package dev.ylp.notes.cli

import dev.ylp.client.auth.domain.Tokens
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.time.Instant

/**
 * Tokens as one line of text, for the stores that keep a single secret: JSON, then Base64, so
 * that no character of it means anything to the tool that stores it.
 */
object TokenCodec {
    fun encode(tokens: Tokens): String =
        Base64.encode(
            Json.encodeToString(Stored(tokens.accessToken, tokens.refreshToken, tokens.expiresAt?.toString())).encodeToByteArray(),
        )

    /** Null when [text] is not tokens this CLI wrote: the user signs in again. */
    fun decode(text: String): Tokens? =
        runCatching {
            val stored = Json.decodeFromString<Stored>(Base64.decode(text.trim()).decodeToString())
            Tokens(stored.accessToken, stored.refreshToken, stored.expiresAt?.let(Instant::parse))
        }.getOrNull()

    @Serializable
    private data class Stored(
        val accessToken: String,
        val refreshToken: String?,
        val expiresAt: String?,
    )
}
