@file:OptIn(ExperimentalForeignApi::class)

package dev.ylp.notes.cli

import dev.ylp.client.auth.domain.Tokens
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.coroutines.runBlocking
import platform.posix.stat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TokenStoresTest {
    private val directory = testDirectory()
    private val tokens = Tokens("access", "refresh", null)

    @Test
    fun aFileKeepsTheTokensReadableByTheOwnerOnly() =
        runBlocking {
            val path = "$directory/tokens"
            val store = FileTokenStore(path)

            store.save(tokens)

            assertEquals(tokens, store.load())
            assertEquals("600", modeOf(path))
            store.clear()
            assertNull(store.load())
        }

    @Test
    fun aFileMadeReadableByOthersIsProtectedAgainOnSave() =
        runBlocking {
            val path = "$directory/tokens"
            writePrivateFile(path, "")
            runForOutput("chmod 644 $path")

            FileTokenStore(path).save(tokens)

            assertEquals("600", modeOf(path))
        }

    @Test
    fun secretToolGetsTheSecretOnItsStandardInputOnly() =
        runBlocking {
            val store = CommandTokenStore.secretTool(fakeTool())

            store.save(tokens)

            assertEquals(tokens, store.load())
            val calls = readFile("$directory/calls").orEmpty()
            assertTrue("store --label=Notes CLI tokens service dev.ylp.notes account tokens" in calls)
            assertFalse(TokenCodec.encode(tokens) in calls)
            store.clear()
            assertNull(store.load())
        }

    @Test
    fun theKeychainGetsTheSecretInsideItsInteractiveCommand() =
        runBlocking {
            val security = fakeTool()

            CommandTokenStore.keychain(security).save(tokens)

            val calls = readFile("$directory/calls").orEmpty()
            assertTrue("-i" in calls)
            assertFalse(TokenCodec.encode(tokens) in calls)
            assertEquals(
                "add-generic-password -U -s dev.ylp.notes -a tokens -w ${TokenCodec.encode(tokens)}\n",
                readFile("$directory/secret"),
            )
        }

    @Test
    fun aToolThatFailsKeepsNoTokens() =
        runBlocking {
            assertNull(CommandTokenStore("false", "false", "false").load())
            assertTrue(runCatching { CommandTokenStore("false", "false", "false").save(tokens) }.isFailure)
        }

    /** A stand-in for a secret tool: it logs its arguments, keeps standard input, and prints it back on lookup. */
    private fun fakeTool(): String {
        val path = "$directory/tool"
        writePrivateFile(
            path,
            """
            #!/bin/sh
            echo "$@" >> $directory/calls
            case "$1" in
              store|-i) cat > $directory/secret ;;
              lookup|find-generic-password) cat $directory/secret 2>/dev/null || exit 1 ;;
              clear|delete-generic-password) rm -f $directory/secret ;;
            esac
            """.trimIndent() + "\n",
        )
        runForOutput("chmod 700 $path")
        return path
    }

    private fun modeOf(path: String): String =
        memScoped {
            val info = alloc<stat>()
            stat(path, info.ptr)
            (info.st_mode.toInt() and 0x1ff).toString(8)
        }
}
