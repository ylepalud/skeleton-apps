package dev.ylp.client.auth.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SecretsTest {
    @Test
    fun `tokens never appear in logs`() {
        val printed = Tokens("access-secret", "refresh-secret", null).toString()

        assertFalse("access-secret" in printed)
        assertFalse("refresh-secret" in printed)
    }

    @Test
    fun `printed tokens say whether they can be refreshed`() {
        assertEquals("Tokens(expiresAt=null, refreshable=false)", Tokens("access", null, null).toString())
    }

    @Test
    fun `the verifier and state of a sign-in never appear in logs`() {
        val printed = AuthorizationRequest("https://idp/authorize", "state-secret", "verifier-secret").toString()

        assertFalse("state-secret" in printed)
        assertFalse("verifier-secret" in printed)
    }
}
