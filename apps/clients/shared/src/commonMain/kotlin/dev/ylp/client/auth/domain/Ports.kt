package dev.ylp.client.auth.domain

/** Where the tokens are kept between launches. Each platform provides a secure store. */
interface TokenStore {
    suspend fun load(): Tokens?

    suspend fun save(tokens: Tokens)

    suspend fun clear()
}

/**
 * Shows the identity provider's sign-in page in a browser the user trusts (Custom Tabs on
 * Android, ASWebAuthenticationSession on iOS). Each platform provides it.
 */
interface AuthorizationBrowser {
    /** Opens [url]; returns the URL the provider redirected to, or null when the user cancelled. */
    suspend fun authorize(
        url: String,
        redirectUri: String,
    ): String?
}

/** The OpenID Connect provider: Authorization Code with PKCE, and refresh. */
interface IdentityProvider {
    suspend fun startAuthorization(): AuthorizationRequest

    /** Checks the redirect against [request] and exchanges its code for tokens. */
    suspend fun completeAuthorization(
        request: AuthorizationRequest,
        redirectUrl: String,
    ): Tokens

    suspend fun refresh(refreshToken: String): Tokens
}
