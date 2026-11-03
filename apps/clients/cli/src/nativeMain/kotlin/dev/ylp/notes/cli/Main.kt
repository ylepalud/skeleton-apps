package dev.ylp.notes.cli

import com.github.ajalt.clikt.command.main
import dev.ylp.client.SharedClient
import dev.ylp.client.openLocalStore
import kotlinx.coroutines.runBlocking
import platform.posix.setenv

/** Builds the client with this computer's adapters, then runs the command. */
fun main(args: Array<String>) = runBlocking { run(args) }

private suspend fun run(args: Array<String>) {
    // The sign-in's one-shot server logs its start to the terminal; warnings only, unless the user asks.
    setenv("KTOR_LOG_LEVEL", "WARN", 0)
    val settings = Settings.from(::environment)
    val config = userDirectory("XDG_CONFIG_HOME", ".config").also(::makeDirectories)
    val data = userDirectory("XDG_DATA_HOME", ".local/share").also(::makeDirectories)
    val client =
        SharedClient(
            apiBaseUrl = settings.apiBaseUrl,
            auth = settings.auth(),
            browser =
                LoopbackBrowser(settings.redirectPort, open = { url ->
                    println("Opening the sign-in page in your browser. If it does not open, visit:\n$url")
                    openInBrowser(url)
                }),
            localStore = openLocalStore(directory = data),
            tokenStore = platformTokenStore(config),
        )
    notesCli(client).main(args)
}

/** Where this platform keeps the tokens, [configDirectory] being the CLI's own folder. */
expect fun platformTokenStore(configDirectory: String): dev.ylp.client.auth.domain.TokenStore

/** Opens [url] in the user's browser, if the system has one. */
expect fun openInBrowser(url: String)
