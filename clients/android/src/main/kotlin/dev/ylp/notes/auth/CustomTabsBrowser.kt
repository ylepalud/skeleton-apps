package dev.ylp.notes.auth

import dev.ylp.client.auth.domain.AuthorizationBrowser
import kotlinx.coroutines.CompletableDeferred

/**
 * Shows the provider's sign-in page in a Custom Tab, then waits for the activity to report how
 * the user came back: through the redirect, or without it (the tab was closed).
 *
 * The activity opens the tab ([open]) and forwards its lifecycle: [onRedirect] from
 * `onNewIntent`, [onPaused] and [onResumed].
 */
class CustomTabsBrowser : AuthorizationBrowser {
    /** Opens a URL in a Custom Tab; set by the activity while it is alive. */
    var open: ((String) -> Unit)? = null

    private var pending: CompletableDeferred<String?>? = null
    private var leftForBrowser = false

    override suspend fun authorize(
        url: String,
        redirectUri: String,
    ): String? {
        val launch = open ?: return null
        val result = CompletableDeferred<String?>()
        pending = result
        leftForBrowser = false
        launch(url)
        return try {
            result.await()
        } finally {
            pending = null
        }
    }

    fun onRedirect(url: String) {
        pending?.complete(url)
    }

    fun onPaused() {
        leftForBrowser = pending != null
    }

    /** Back in the app without a redirect: the user closed the tab. */
    fun onResumed() {
        if (leftForBrowser) pending?.complete(null)
        leftForBrowser = false
    }
}
