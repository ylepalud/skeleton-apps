package dev.ylp.notes.cli

import dev.ylp.client.auth.domain.AuthConfig

/**
 * Where the CLI finds the API and the identity provider, from environment variables; the
 * defaults target the local stack.
 */
data class Settings(
    val apiBaseUrl: String,
    val issuer: String,
    val clientId: String,
    /** The loopback port of the sign-in redirect, when the provider has no device flow (RFC 8252). */
    val redirectPort: Int,
) {
    val redirectUri: String get() = "http://127.0.0.1:$redirectPort/callback"

    fun auth() = AuthConfig(issuer, clientId, redirectUri)

    companion object {
        fun from(environment: (String) -> String?) =
            Settings(
                apiBaseUrl = environment("NOTES_API_BASE_URL") ?: "http://localhost:8088/api/v1",
                issuer = environment("NOTES_OIDC_ISSUER") ?: "http://localhost:8090/default",
                clientId = environment("NOTES_OIDC_CLIENT_ID") ?: "api-cli",
                redirectPort = environment("NOTES_OIDC_REDIRECT_PORT")?.toIntOrNull() ?: DEFAULT_REDIRECT_PORT,
            )

        const val DEFAULT_REDIRECT_PORT = 8770
    }
}
