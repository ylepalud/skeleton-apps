package dev.ylp.notes.cli

import dev.ylp.client.auth.domain.AuthorizationBrowser
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.queryString
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * The sign-in of a native app on a desktop (RFC 8252): the provider's page opens in the user's
 * browser, and its redirect comes back to a one-shot server on 127.0.0.1. [open] opens a URL in
 * the browser; null when the user did not finish within [timeout].
 */
class LoopbackBrowser(
    private val port: Int,
    private val open: suspend (String) -> Unit,
    private val timeout: Duration = 5.minutes,
) : AuthorizationBrowser {
    override suspend fun authorize(
        url: String,
        redirectUri: String,
    ): String? {
        val redirect = CompletableDeferred<String>()
        val server =
            embeddedServer(CIO, host = "127.0.0.1", port = port) {
                routing {
                    get("/callback") {
                        call.respondText("Signed in. You can close this tab and go back to the terminal.")
                        redirect.complete("$redirectUri?${call.request.queryString()}")
                    }
                }
            }
        server.startSuspend(wait = false)
        return try {
            open(url)
            withTimeoutOrNull(timeout) { redirect.await() }
        } finally {
            server.stopSuspend(gracePeriodMillis = 200, timeoutMillis = 1_000)
        }
    }
}
