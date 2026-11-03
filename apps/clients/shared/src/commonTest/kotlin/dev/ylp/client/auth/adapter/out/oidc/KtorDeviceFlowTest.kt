package dev.ylp.client.auth.adapter.out.oidc

import dev.ylp.client.auth.domain.AuthConfig
import dev.ylp.client.auth.domain.AuthenticationException
import dev.ylp.client.auth.domain.Authorization
import dev.ylp.client.auth.domain.DeviceAuthorization
import dev.ylp.client.auth.domain.DevicePoll
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
import io.ktor.http.headersOf
import io.ktor.http.parseQueryString
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** The device flow (RFC 8628) against a provider that offers it, and one that does not. */
class KtorDeviceFlowTest {
    private val config = AuthConfig("https://idp.example/default", "cli", "http://127.0.0.1/callback")
    private val forms = mutableListOf<Pair<String, Map<String, String>>>()

    private fun provider(
        deviceEndpoint: Boolean = true,
        answer: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): KtorIdentityProvider {
        val engine =
            MockEngine { request ->
                when (request.url.encodedPath) {
                    "/default/.well-known/openid-configuration" -> {
                        val device = if (deviceEndpoint) DEVICE_ENDPOINT else ""
                        json(
                            """{"authorization_endpoint": "https://idp.example/default/authorize",
                                "token_endpoint": "https://idp.example/default/token"$device}""",
                        )
                    }

                    else -> {
                        forms += request.url.encodedPath to
                            parseQueryString(
                                request.body.toByteArray().decodeToString(),
                            ).entries().associate { it.key to it.value.single() }
                        answer(request)
                    }
                }
            }
        return KtorIdentityProvider(config, HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } })
    }

    @Test
    fun `a device authorization asks for the client's scopes and returns the code to show`() =
        runTest {
            val authorization =
                provider {
                    json(
                        """{"device_code": "secret", "user_code": "WDJB-MJHT", "verification_uri": "https://idp.example/device",
                            "verification_uri_complete": "https://idp.example/device?user_code=WDJB-MJHT", "expires_in": 600, "interval": 3}""",
                    )
                }.startDeviceAuthorization()

            assertEquals(
                DeviceAuthorization(
                    "secret",
                    "WDJB-MJHT",
                    "https://idp.example/device",
                    "https://idp.example/device?user_code=WDJB-MJHT",
                    3,
                ),
                authorization,
            )
            assertEquals("/default/device" to mapOf("client_id" to "cli", "scope" to "openid offline_access"), forms.single())
        }

    @Test
    fun `a provider naming no interval is polled every 5 seconds`() =
        runTest {
            val authorization =
                provider { json("""{"device_code": "secret", "user_code": "CODE", "verification_uri": "https://idp.example/device"}""") }
                    .startDeviceAuthorization()

            assertEquals(5, authorization?.intervalSeconds)
        }

    @Test
    fun `a provider without the device flow offers none`() =
        runTest {
            assertNull(provider(deviceEndpoint = false) { error("never called") }.startDeviceAuthorization())
        }

    @Test
    fun `a refused device authorization fails with the provider's reason`() =
        runTest {
            val failure =
                assertFailsWith<AuthenticationException> {
                    provider { json("""{"error": "invalid_client", "error_description": "Unknown client"}""", HttpStatusCode.BadRequest) }
                        .startDeviceAuthorization()
                }
            assertEquals("Unknown client", failure.message)
        }

    @Test
    fun `the poll sends the device code and reads the tokens once granted`() =
        runTest {
            val poll =
                provider { json("""{"access_token": "access", "refresh_token": "refresh", "id_token": "h.eyJzdWIiOiJhbGljZSJ9.s"}""") }
                    .pollDeviceToken(DEVICE)

            assertEquals(DevicePoll.Granted(Authorization(Tokens("access", "refresh", null), "alice")), poll)
            assertEquals(
                "/default/token" to
                    mapOf("grant_type" to "urn:ietf:params:oauth:grant-type:device_code", "device_code" to "secret", "client_id" to "cli"),
                forms.single(),
            )
        }

    @Test
    fun `the poll reads each answer of a provider still waiting`() =
        runTest {
            suspend fun pollAnswering(error: String) =
                provider { json("""{"error": "$error"}""", HttpStatusCode.BadRequest) }.pollDeviceToken(DEVICE)

            assertEquals(DevicePoll.Pending, pollAnswering("authorization_pending"))
            assertEquals(DevicePoll.SlowDown, pollAnswering("slow_down"))
            assertEquals(DevicePoll.Denied, pollAnswering("access_denied"))
            assertEquals(DevicePoll.Expired, pollAnswering("expired_token"))
            assertFailsWith<AuthenticationException> { pollAnswering("invalid_grant") }
        }

    private fun MockRequestHandleScope.json(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private companion object {
        const val DEVICE_ENDPOINT = """, "device_authorization_endpoint": "https://idp.example/default/device""""
        val DEVICE = DeviceAuthorization("secret", "WDJB-MJHT", "https://idp.example/device", null, 5)
    }
}
