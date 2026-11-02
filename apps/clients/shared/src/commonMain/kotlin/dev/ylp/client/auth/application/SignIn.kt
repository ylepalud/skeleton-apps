package dev.ylp.client.auth.application

import dev.ylp.client.auth.domain.AuthenticationException
import dev.ylp.client.auth.domain.AuthorizationBrowser
import dev.ylp.client.auth.domain.DeviceData
import dev.ylp.client.auth.domain.IdentityProvider
import dev.ylp.client.auth.domain.TokenStore

/**
 * Signs the user in through the provider's page, then keeps the tokens. The data kept on the
 * device stays when the same user signs in again, with the changes not sent yet.
 */
class SignIn internal constructor(
    private val identityProvider: IdentityProvider,
    private val browser: AuthorizationBrowser,
    private val tokens: TokenStore,
    private val deviceData: DeviceData,
    private val redirectUri: String,
) {
    suspend fun execute(): SignInResult =
        try {
            val request = identityProvider.startAuthorization()
            val redirect = browser.authorize(request.url, redirectUri)
            if (redirect == null) {
                SignInResult.Cancelled
            } else {
                val authorization = identityProvider.completeAuthorization(request, redirect)
                deviceData.claimFor(authorization.subject)
                tokens.save(authorization.tokens)
                SignInResult.SignedIn
            }
        } catch (e: AuthenticationException) {
            SignInResult.Failed(e.message)
        }
}

/** The outcome of a sign-in. */
sealed class SignInResult {
    data object SignedIn : SignInResult()

    data object Cancelled : SignInResult()

    data class Failed(
        val message: String,
    ) : SignInResult()
}
