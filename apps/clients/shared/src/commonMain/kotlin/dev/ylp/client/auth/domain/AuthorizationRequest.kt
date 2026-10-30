package dev.ylp.client.auth.domain

/**
 * A sign-in in progress: the URL to open, and the secrets the redirect is checked against.
 * Never printed: [toString] hides the secrets.
 */
data class AuthorizationRequest(
    val url: String,
    val state: String,
    val codeVerifier: String,
) {
    override fun toString(): String = "AuthorizationRequest(url=$url)"
}

/** The identity provider refused a request, or answered something unusable. */
class AuthenticationException(
    override val message: String,
) : Exception(message)
