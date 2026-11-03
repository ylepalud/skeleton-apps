package dev.ylp.notes.cli

import dev.ylp.client.auth.domain.Tokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class SettingsTest {
    @Test
    fun theDefaultsTargetTheLocalStack() {
        val settings = Settings.from { null }

        assertEquals("http://localhost:8088/api/v1", settings.apiBaseUrl)
        assertEquals("http://localhost:8090/default", settings.issuer)
        assertEquals("api-cli", settings.clientId)
        assertEquals("http://127.0.0.1:8770/callback", settings.redirectUri)
    }

    @Test
    fun environmentVariablesPointElsewhere() {
        val environment =
            mapOf(
                "NOTES_API_BASE_URL" to "https://notes.example/api/v1",
                "NOTES_OIDC_ISSUER" to "https://idp.example",
                "NOTES_OIDC_CLIENT_ID" to "cli",
                "NOTES_OIDC_REDIRECT_PORT" to "9000",
            )

        val settings = Settings.from(environment::get)

        assertEquals(Settings("https://notes.example/api/v1", "https://idp.example", "cli", 9000), settings)
        assertEquals("cli", settings.auth().clientId)
    }

    @Test
    fun tokensSurviveTheirTextForm() {
        val tokens = Tokens("access", "refresh", Instant.parse("2026-11-03T09:00:00Z"))

        assertEquals(tokens, TokenCodec.decode(TokenCodec.encode(tokens)))
        assertEquals(Tokens("access", null, null), TokenCodec.decode(TokenCodec.encode(Tokens("access", null, null))))
        assertNull(TokenCodec.decode("not tokens"))
    }
}
