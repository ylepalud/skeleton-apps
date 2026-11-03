package dev.ylp.notes.cli

import dev.ylp.client.auth.domain.TokenStore
import dev.ylp.client.auth.domain.Tokens
import platform.posix.unlink

/**
 * Tokens kept by a tool of the system's secret store, through the shell: [store] reads on its
 * standard input what [input] makes of the secret, [lookup] prints the secret, [clear] forgets it.
 * The secret never appears in a command line, which other users of the computer can read.
 */
class CommandTokenStore(
    private val store: String,
    private val lookup: String,
    private val clear: String,
    private val input: (String) -> String = { it },
) : TokenStore {
    override suspend fun load(): Tokens? = runForOutput(lookup)?.let(TokenCodec::decode)

    override suspend fun save(tokens: Tokens) {
        if (!runWithInput(store, input(TokenCodec.encode(tokens)))) error("The secret store refused the tokens")
    }

    override suspend fun clear() {
        runForOutput(clear)
    }

    companion object {
        private const val SERVICE = "dev.ylp.notes"
        private const val ACCOUNT = "tokens"

        /**
         * The macOS Keychain, through `security`: its interactive mode reads a command from standard
         * input, so the secret is written there, inside the command, never in the arguments.
         */
        fun keychain(security: String = "/usr/bin/security") =
            CommandTokenStore(
                store = "$security -i >/dev/null",
                lookup = "$security find-generic-password -s $SERVICE -a $ACCOUNT -w 2>/dev/null",
                clear = "$security delete-generic-password -s $SERVICE -a $ACCOUNT >/dev/null 2>&1",
                input = { secret -> "add-generic-password -U -s $SERVICE -a $ACCOUNT -w $secret\n" },
            )

        /** libsecret, through `secret-tool`, which reads the secret from standard input. */
        fun secretTool(secretTool: String = "secret-tool") =
            CommandTokenStore(
                store = "$secretTool store --label='Notes CLI tokens' service $SERVICE account $ACCOUNT",
                lookup = "$secretTool lookup service $SERVICE account $ACCOUNT 2>/dev/null",
                clear = "$secretTool clear service $SERVICE account $ACCOUNT >/dev/null 2>&1",
            )
    }
}

/** Tokens in a file readable and writable by the owner only, when the system has no secret store. */
class FileTokenStore(
    private val path: String,
) : TokenStore {
    override suspend fun load(): Tokens? = readFile(path)?.let(TokenCodec::decode)

    override suspend fun save(tokens: Tokens) = writePrivateFile(path, TokenCodec.encode(tokens))

    override suspend fun clear() {
        unlink(path)
    }
}
