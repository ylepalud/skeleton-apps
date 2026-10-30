package dev.ylp.notes.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.ylp.client.auth.domain.Tokens
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Instant

/** The store with a reversible fake cipher: the AndroidKeyStore exists on devices only. */
@RunWith(RobolectricTestRunner::class)
class KeystoreTokenStoreTest {
    private val preferences = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("test", Context.MODE_PRIVATE)
    private val store = KeystoreTokenStore(preferences, ReversingCipher)

    @Test
    fun savedTokensAreReadBack() = runTest {
        val tokens = Tokens("access", "refresh", Instant.parse("2026-10-29T10:00:00Z"))

        store.save(tokens)

        assertEquals(tokens, store.load())
    }

    @Test
    fun tokensWithoutRefreshOrExpiryAreReadBack() = runTest {
        store.save(Tokens("access", null, null))

        assertEquals(Tokens("access", null, null), store.load())
    }

    @Test
    fun tokensAreNeverStoredInClear() = runTest {
        store.save(Tokens("access-secret", "refresh-secret", null))

        assertFalse(preferences.all.values.joinToString().contains("secret"))
    }

    @Test
    fun clearedTokensAreGone() = runTest {
        store.save(Tokens("access", "refresh", null))

        store.clear()

        assertNull(store.load())
    }

    @Test
    fun unreadableTokensAreDropped() = runTest {
        preferences.edit().putString("tokens", "not encrypted").commit()

        assertNull(store.load())
        assertFalse(preferences.contains("tokens"))
    }

    @Test
    fun nothingStoredReadsAsSignedOut() = runTest {
        assertNull(store.load())
    }

    private object ReversingCipher : TokenCipher {
        override fun encrypt(plain: ByteArray) = plain.reversedArray().map { (it + 1).toByte() }.toByteArray()

        override fun decrypt(encrypted: ByteArray) = encrypted.map { (it - 1).toByte() }.toByteArray().reversedArray()
    }
}
