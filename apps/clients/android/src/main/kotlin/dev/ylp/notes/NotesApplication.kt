package dev.ylp.notes

import android.app.Application
import android.content.Context
import dev.ylp.client.SharedClient
import dev.ylp.client.auth.domain.AuthConfig
import dev.ylp.notes.auth.CustomTabsBrowser
import dev.ylp.notes.auth.KeystoreCipher
import dev.ylp.notes.auth.KeystoreTokenStore

/** Builds the shared client once, with this platform's adapters. */
open class NotesApplication : Application() {
    val browser = CustomTabsBrowser()

    val client: SharedClient by lazy { createClient() }

    /** The client of the app; tests point it at a mock server. */
    protected open fun createClient(): SharedClient = SharedClient(
        apiBaseUrl = BuildConfig.API_BASE_URL,
        auth = AuthConfig(BuildConfig.OIDC_ISSUER, BuildConfig.OIDC_CLIENT_ID, BuildConfig.OIDC_REDIRECT_URI),
        browser = browser,
        tokenStore = KeystoreTokenStore(getSharedPreferences("session", Context.MODE_PRIVATE), KeystoreCipher()),
    )
}
