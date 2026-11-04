@file:OptIn(ExperimentalForeignApi::class)

package dev.ylp.notes.cli

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlinx.cinterop.toKString
import platform.posix.EEXIST
import platform.posix.O_CREAT
import platform.posix.O_TRUNC
import platform.posix.O_WRONLY
import platform.posix.S_IRUSR
import platform.posix.S_IWUSR
import platform.posix.close
import platform.posix.errno
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen
import platform.posix.fputs
import platform.posix.getenv
import platform.posix.open
import platform.posix.pclose
import platform.posix.popen
import platform.posix.write

/** The value of an environment variable, or null when it is unset or empty. */
fun environment(name: String): String? = getenv(name)?.toKString()?.takeIf { it.isNotEmpty() }

/** Creates [path] and its parents, readable by the owner only. */
fun makeDirectories(path: String) {
    var current = ""
    path.split('/').filter { it.isNotEmpty() }.forEach { part ->
        current += "/$part"
        if (makePrivateDirectory(current) != 0 && errno != EEXIST) error("Cannot create $current")
    }
}

/** A folder of the user's, following the XDG base directories: [variable], or [fallback] under the home folder. */
fun userDirectory(
    variable: String,
    fallback: String,
): String = (environment(variable) ?: "${environment("HOME") ?: error("HOME is not set")}/$fallback") + "/notes"

/** Runs [command] through the shell, writing [input] to its standard input; true when it exits with 0. */
fun runWithInput(
    command: String,
    input: String,
): Boolean {
    val process = popen(command, "w") ?: return false
    fputs(input, process)
    return exitCode(pclose(process)) == 0
}

/** Runs [command] through the shell; its standard output, or null when it does not exit with 0. */
fun runForOutput(command: String): String? {
    val process = popen(command, "r") ?: return null
    val output = StringBuilder()
    val buffer = ByteArray(4096)
    while (fgets(buffer.refTo(0), buffer.size, process) != null) output.append(buffer.toKString())
    return if (exitCode(pclose(process)) == 0) output.toString() else null
}

// pclose answers the wait status: the exit code is its second byte.
private fun exitCode(status: Int) = (status shr 8) and 0xff

/** Writes [text] to [path], readable and writable by the owner only, whatever the umask. */
fun writePrivateFile(
    path: String,
    text: String,
) {
    val descriptor = open(path, O_WRONLY or O_CREAT or O_TRUNC, S_IRUSR or S_IWUSR)
    if (descriptor < 0) error("Cannot write $path")
    try {
        // A file that existed keeps its mode through open: set it again.
        if (makePrivateFile(descriptor) != 0) error("Cannot protect $path")
        val bytes = text.encodeToByteArray()
        if (bytes.isNotEmpty() && write(descriptor, bytes.refTo(0), bytes.size.toULong()) < 0) error("Cannot write $path")
    } finally {
        close(descriptor)
    }
}

/** The text of [path], or null when it cannot be read. */
fun readFile(path: String): String? {
    val file = fopen(path, "r") ?: return null
    val output = StringBuilder()
    val buffer = ByteArray(4096)
    try {
        while (fgets(buffer.refTo(0), buffer.size, file) != null) output.append(buffer.toKString())
    } finally {
        fclose(file)
    }
    return output.toString()
}

/** Quotes [value] for the shell. */
fun shellQuote(value: String) = "'" + value.replace("'", "'\\''") + "'"
