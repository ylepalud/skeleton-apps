package dev.ylp.client.auth.adapter.out.oidc

import dev.ylp.client.auth.domain.AuthConfig
import dev.ylp.client.auth.domain.AuthenticationException
import dev.ylp.client.auth.domain.AuthorizationRequest
import dev.ylp.client.auth.domain.Tokens
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.http.parseQueryString
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.time.Clock
import kotlin.time.Instant

class KtorIdentityProviderTest {
    private val config = AuthConfig("https://idp.example/default", "mobile", "app://callback")
    private val now = Instant.parse("2026-10-29T10:00:00Z")
    private val tokenForms = mutableListOf<Map<String, String>>()

    private fun provider(token: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): KtorIdentityProvider {
        val engine =
            MockEngine { request ->
                when (request.url.encodedPath) {
                    "/default/.well-known/openid-configuration" -> {
                        json(
                            """{"authorization_endpoint": "https://idp.example/default/authorize",
                                "token_endpoint": "https://idp.example/default/token", "issuer": "https://idp.example/default"}""",
                        )
                    }

                    "/default/token" -> {
                        tokenForms +=
                            parseQueryString(
                                request.body.toByteArray().decodeToString(),
                            ).entries().associate { it.key to it.value.single() }
                        token(request)
                    }

                    else -> {
                        respond("", HttpStatusCode.NotFound)
                    }
                }
            }
        val http = HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        return KtorIdentityProvider(config, http, FixedClock(now))
    }

    @Test
    fun `the S256 challenge matches the RFC 7636 example`() {
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            KtorIdentityProvider.challengeOf("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"),
        )
    }

    @Test
    fun `a sign-in opens the authorization endpoint with a PKCE challenge of its verifier`() =
        runTest {
            val request = provider { tokens() }.startAuthorization()

            val url = Url(request.url)
            assertEquals("https://idp.example/default/authorize", url.toString().substringBefore('?'))
            assertEquals("code", url.parameters["response_type"])
            assertEquals("mobile", url.parameters["client_id"])
            assertEquals("app://callback", url.parameters["redirect_uri"])
            assertEquals("openid offline_access", url.parameters["scope"])
            assertEquals(request.state, url.parameters["state"])
            assertEquals("S256", url.parameters["code_challenge_method"])
            assertEquals(KtorIdentityProvider.challengeOf(request.codeVerifier), url.parameters["code_challenge"])
            assertEquals(43, request.codeVerifier.length)
        }

    @Test
    fun `each sign-in gets its own verifier and state`() =
        runTest {
            val provider = provider { tokens() }

            val first = provider.startAuthorization()
            val second = provider.startAuthorization()

            assertNotEquals(first.codeVerifier, second.codeVerifier)
            assertNotEquals(first.state, second.state)
        }

    @Test
    fun `the code of the redirect is exchanged with the verifier`() =
        runTest {
            val tokens = provider { tokens() }.completeAuthorization(REQUEST, "app://callback?code=the-code&state=the-state")

            assertEquals(Tokens("access", "refresh", now + kotlin.time.Duration.parse("5m")), tokens)
            assertEquals(
                mapOf(
                    "grant_type" to "authorization_code",
                    "code" to "the-code",
                    "redirect_uri" to "app://callback",
                    "client_id" to "mobile",
                    "code_verifier" to "the-verifier",
                ),
                tokenForms.single(),
            )
        }

    @Test
    fun `a redirect with another state is refused`() =
        runTest {
            assertFailsWith<AuthenticationException> {
                provider { tokens() }.completeAuthorization(REQUEST, "app://callback?code=the-code&state=forged")
            }
            assertEquals(emptyList(), tokenForms)
        }

    @Test
    fun `a redirect carrying an error is refused with its description`() =
        runTest {
            val failure =
                assertFailsWith<AuthenticationException> {
                    provider { tokens() }.completeAuthorization(REQUEST, "app://callback?error=access_denied&error_description=Denied")
                }
            assertEquals("Denied", failure.message)
        }

    @Test
    fun `a redirect without a code is refused`() =
        runTest {
            assertFailsWith<AuthenticationException> {
                provider { tokens() }.completeAuthorization(REQUEST, "app://callback?state=the-state")
            }
        }

    @Test
    fun `a refresh keeps the refresh token the provider does not send again`() =
        runTest {
            val tokens = provider { json("""{"access_token": "new-access"}""") }.refresh("the-refresh")

            assertEquals(Tokens("new-access", "the-refresh", null), tokens)
            assertEquals("refresh_token", tokenForms.single()["grant_type"])
            assertEquals("the-refresh", tokenForms.single()["refresh_token"])
        }

    @Test
    fun `a refused token request reports the provider's error`() =
        runTest {
            val failure =
                assertFailsWith<AuthenticationException> {
                    provider { json("""{"error": "invalid_grant", "error_description": "Expired"}""", HttpStatusCode.BadRequest) }
                        .refresh("the-refresh")
                }
            assertEquals("Expired", failure.message)
        }

    @Test
    fun `a refused token request without description reports the error code`() =
        runTest {
            val failure =
                assertFailsWith<AuthenticationException> {
                    provider { json("""{"error": "invalid_grant"}""", HttpStatusCode.BadRequest) }.refresh("the-refresh")
                }
            assertEquals("invalid_grant", failure.message)
        }

    @Test
    fun `a refused token request without an OAuth error reports the status`() =
        runTest {
            val failure =
                assertFailsWith<AuthenticationException> {
                    provider { respond("", HttpStatusCode.ServiceUnavailable) }.refresh("the-refresh")
                }
            assertEquals("Token request failed: 503", failure.message)
        }

    @Test
    fun `an unreachable provider fails the sign-in`() =
        runTest {
            val http = HttpClient(MockEngine { throw IllegalStateException("offline") })
            val provider = KtorIdentityProvider(config, http, FixedClock(now))

            assertFailsWith<AuthenticationException> { provider.startAuthorization() }
        }

    @Test
    fun `a provider without discovery fails the sign-in`() =
        runTest {
            val http = HttpClient(MockEngine { respond("", HttpStatusCode.NotFound) })
            val provider = KtorIdentityProvider(config, http, FixedClock(now))

            assertFailsWith<AuthenticationException> { provider.startAuthorization() }
        }

    private fun MockRequestHandleScope.tokens() = json("""{"access_token": "access", "refresh_token": "refresh", "expires_in": 300}""")

    private fun MockRequestHandleScope.json(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private class FixedClock(
        private val instant: Instant,
    ) : Clock {
        override fun now() = instant
    }

    private companion object {
        val REQUEST = AuthorizationRequest("https://idp.example/default/authorize", "the-state", "the-verifier")
    }
}
