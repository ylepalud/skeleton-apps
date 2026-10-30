package dev.ylp.client.auth.adapter.out.oidc

import dev.ylp.client.auth.domain.AuthConfig
import dev.ylp.client.auth.domain.AuthenticationException
import dev.ylp.client.auth.domain.AuthorizationRequest
import dev.ylp.client.auth.domain.IdentityProvider
import dev.ylp.client.auth.domain.Tokens
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.kotlincrypto.hash.sha2.SHA256
import org.kotlincrypto.random.CryptoRand
import kotlin.io.encoding.Base64
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * Authorization Code with PKCE (RFC 7636, S256) and refresh against an OpenID Connect provider,
 * whose endpoints come from its discovery document.
 *
 * [http] must decode JSON; it carries no user token.
 */
internal class KtorIdentityProvider(
    private val config: AuthConfig,
    private val http: HttpClient,
    private val clock: Clock = Clock.System,
) : IdentityProvider {
    private var discovery: Discovery? = null

    override suspend fun startAuthorization(): AuthorizationRequest {
        val verifier = randomToken(VERIFIER_BYTES)
        val state = randomToken(STATE_BYTES)
        val url =
            URLBuilder(discover().authorizationEndpoint)
                .apply {
                    parameters.append("response_type", "code")
                    parameters.append("client_id", config.clientId)
                    parameters.append("redirect_uri", config.redirectUri)
                    parameters.append("scope", config.scopes.joinToString(" "))
                    parameters.append("state", state)
                    parameters.append("code_challenge", challengeOf(verifier))
                    parameters.append("code_challenge_method", "S256")
                }.buildString()
        return AuthorizationRequest(url, state, verifier)
    }

    override suspend fun completeAuthorization(
        request: AuthorizationRequest,
        redirectUrl: String,
    ): Tokens {
        val redirect = Url(redirectUrl).parameters
        redirect["error"]?.let { throw AuthenticationException(redirect["error_description"] ?: it) }
        // A state that differs means the redirect does not answer this request: a forged one.
        if (redirect["state"] != request.state) throw AuthenticationException("The sign-in answer does not match the request")
        val code = redirect["code"] ?: throw AuthenticationException("The sign-in answer has no code")
        return requestTokens(
            parameters {
                append("grant_type", "authorization_code")
                append("code", code)
                append("redirect_uri", config.redirectUri)
                append("client_id", config.clientId)
                append("code_verifier", request.codeVerifier)
            },
            previousRefreshToken = null,
        )
    }

    override suspend fun refresh(refreshToken: String): Tokens =
        requestTokens(
            parameters {
                append("grant_type", "refresh_token")
                append("refresh_token", refreshToken)
                append("client_id", config.clientId)
            },
            // A provider may keep the refresh token and not send it again.
            previousRefreshToken = refreshToken,
        )

    private suspend fun requestTokens(
        form: io.ktor.http.Parameters,
        previousRefreshToken: String?,
    ): Tokens {
        val response = call { http.submitForm(discover().tokenEndpoint, form) }
        if (!response.status.isSuccess()) {
            val error = runCatching { response.body<TokenError>() }.getOrNull()
            throw AuthenticationException(error?.description ?: error?.error ?: "Token request failed: ${response.status.value}")
        }
        val body = response.body<TokenResponse>()
        return Tokens(
            accessToken = body.accessToken,
            refreshToken = body.refreshToken ?: previousRefreshToken,
            expiresAt = body.expiresIn?.let { clock.now() + it.seconds },
        )
    }

    private suspend fun discover(): Discovery =
        discovery ?: run {
            val response = call { http.get("${config.issuer.trimEnd('/')}/.well-known/openid-configuration") }
            if (!response.status.isSuccess()) throw AuthenticationException("Discovery failed: ${response.status.value}")
            response.body<Discovery>().also { discovery = it }
        }

    /** Turns a transport failure into an [AuthenticationException], cancellation excepted. */
    private suspend fun call(request: suspend () -> HttpResponse): HttpResponse =
        try {
            request()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw AuthenticationException("The identity provider cannot be reached: ${e.message}")
        }

    @Serializable
    private data class Discovery(
        @SerialName("authorization_endpoint") val authorizationEndpoint: String,
        @SerialName("token_endpoint") val tokenEndpoint: String,
    )

    @Serializable
    private data class TokenResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("refresh_token") val refreshToken: String? = null,
        @SerialName("expires_in") val expiresIn: Long? = null,
    )

    @Serializable
    private data class TokenError(
        val error: String,
        @SerialName("error_description") val description: String? = null,
    )

    companion object {
        // 32 bytes give a 43-character verifier, the shortest RFC 7636 accepts.
        private const val VERIFIER_BYTES = 32
        private const val STATE_BYTES = 16
        private val base64Url = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

        private fun randomToken(bytes: Int): String = base64Url.encode(CryptoRand.Default.nextBytes(ByteArray(bytes)))

        /** The S256 challenge of a verifier: BASE64URL(SHA256(ASCII(verifier))). */
        fun challengeOf(verifier: String): String = base64Url.encode(SHA256().digest(verifier.encodeToByteArray()))
    }
}
