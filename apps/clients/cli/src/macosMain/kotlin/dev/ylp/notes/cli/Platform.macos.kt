package dev.ylp.notes.cli

import dev.ylp.client.auth.domain.TokenStore
import platform.posix.S_IRUSR
import platform.posix.S_IRWXU
import platform.posix.S_IWUSR
import platform.posix.fchmod
import platform.posix.mkdir

actual fun platformTokenStore(configDirectory: String): TokenStore = CommandTokenStore.keychain()

actual fun openInBrowser(url: String) {
    runForOutput("open ${shellQuote(url)} >/dev/null 2>&1")
}

internal actual fun makePrivateDirectory(path: String): Int = mkdir(path, S_IRWXU.toUShort())

internal actual fun makePrivateFile(descriptor: Int): Int = fchmod(descriptor, (S_IRUSR or S_IWUSR).toUShort())
