package dev.ylp.client.auth.domain

/**
 * The OpenID Connect client a native app is registered as: a public client, so no secret.
 *
 * [redirectUri] is where the provider sends the browser back: a custom scheme or an app link
 * the platform routes to the app.
 */
data class AuthConfig(
    val issuer: String,
    val clientId: String,
    val redirectUri: String,
    val scopes: List<String> = listOf("openid", "offline_access"),
)
