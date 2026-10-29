package dev.ylp.notes.auth

import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import dev.ylp.client.auth.domain.TokenStore
import dev.ylp.client.auth.domain.Tokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.time.Instant

/** Encrypts and decrypts the stored tokens. */
interface TokenCipher {
    fun encrypt(plain: ByteArray): ByteArray

    fun decrypt(encrypted: ByteArray): ByteArray
}

/**
 * Keeps the tokens between launches, encrypted by [cipher] (a key of the AndroidKeyStore on a
 * device). Data it cannot read, such as after the key was lost, is dropped: the user signs in again.
 */
class KeystoreTokenStore(
    private val preferences: SharedPreferences,
    private val cipher: TokenCipher,
) : TokenStore {
    override suspend fun load(): Tokens? = withContext(Dispatchers.IO) {
        val stored = preferences.getString(KEY, null) ?: return@withContext null
        try {
            val json = JSONObject(cipher.decrypt(Base64.decode(stored, Base64.NO_WRAP)).decodeToString())
            Tokens(
                accessToken = json.getString("accessToken"),
                refreshToken = json.optString("refreshToken").ifEmpty { null },
                expiresAt = json.optLong("expiresAt", NO_EXPIRY).takeIf { it != NO_EXPIRY }?.let { Instant.fromEpochMilliseconds(it) },
            )
        } catch (e: Exception) {
            preferences.edit { remove(KEY) }
            null
        }
    }

    override suspend fun save(tokens: Tokens) = withContext(Dispatchers.IO) {
        val json =
            JSONObject()
                .put("accessToken", tokens.accessToken)
                .put("refreshToken", tokens.refreshToken ?: "")
                .put("expiresAt", tokens.expiresAt?.toEpochMilliseconds() ?: NO_EXPIRY)
        val encrypted = Base64.encodeToString(cipher.encrypt(json.toString().encodeToByteArray()), Base64.NO_WRAP)
        preferences.edit { putString(KEY, encrypted) }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        preferences.edit { remove(KEY) }
    }

    private companion object {
        const val KEY = "tokens"
        const val NO_EXPIRY = -1L
    }
}
