package dev.ylp.notes.cli

import dev.ylp.client.auth.domain.TokenStore
import platform.posix.S_IRUSR
import platform.posix.S_IRWXU
import platform.posix.S_IWUSR
import platform.posix.fchmod
import platform.posix.mkdir

/** libsecret when `secret-tool` and a session bus are there, a private file otherwise (a server, a container). */
actual fun platformTokenStore(configDirectory: String): TokenStore =
    if (environment("DBUS_SESSION_BUS_ADDRESS") != null && runForOutput("command -v secret-tool") != null) {
        CommandTokenStore.secretTool()
    } else {
        FileTokenStore("$configDirectory/tokens")
    }

actual fun openInBrowser(url: String) {
    runForOutput("xdg-open ${shellQuote(url)} >/dev/null 2>&1")
}

internal actual fun makePrivateDirectory(path: String): Int = mkdir(path, S_IRWXU.toUInt())

internal actual fun makePrivateFile(descriptor: Int): Int = fchmod(descriptor, (S_IRUSR or S_IWUSR).toUInt())
