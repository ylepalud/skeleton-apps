package dev.ylp.notes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import dev.ylp.notes.ui.NotesApp

/** The only activity: the Compose screens, and the end of the sign-in redirect. */
class MainActivity : ComponentActivity() {
    private val app get() = application as NotesApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.browser.open = { url -> CustomTabsIntent.Builder().build().launchUrl(this, url.toUri()) }
        setContent { NotesApp(app.client) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { app.browser.onRedirect(it.toString()) }
    }

    override fun onPause() {
        super.onPause()
        app.browser.onPaused()
    }

    override fun onResume() {
        super.onResume()
        app.browser.onResumed()
    }

    override fun onDestroy() {
        app.browser.open = null
        super.onDestroy()
    }
}
