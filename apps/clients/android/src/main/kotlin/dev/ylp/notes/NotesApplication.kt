package dev.ylp.notes

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import dev.ylp.client.LocalStore
import dev.ylp.client.SharedClient
import dev.ylp.client.auth.domain.AuthConfig
import dev.ylp.client.openLocalStore
import dev.ylp.notes.auth.CustomTabsBrowser
import dev.ylp.notes.auth.KeystoreCipher
import dev.ylp.notes.auth.KeystoreTokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Builds the shared client once, with this platform's adapters, and syncs when the network returns. */
open class NotesApplication : Application() {
    val browser = CustomTabsBrowser()

    val client: SharedClient by lazy { createClient() }

    private val scope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        // The changes made offline leave as soon as a network is back.
        getSystemService(ConnectivityManager::class.java)?.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    scope.launch { client.syncChanges.execute() }
                }
            },
        )
    }

    /** Where the notes and the changes not sent yet are kept; tests keep them in memory. */
    protected open fun createLocalStore(): LocalStore = openLocalStore(this)

    /** The client of the app; tests point it at a mock server. */
    protected open fun createClient(): SharedClient = SharedClient(
        apiBaseUrl = BuildConfig.API_BASE_URL,
        auth = AuthConfig(BuildConfig.OIDC_ISSUER, BuildConfig.OIDC_CLIENT_ID, BuildConfig.OIDC_REDIRECT_URI),
        browser = browser,
        localStore = createLocalStore(),
        tokenStore = KeystoreTokenStore(getSharedPreferences("session", Context.MODE_PRIVATE), KeystoreCipher()),
    )
}
