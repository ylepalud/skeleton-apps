package dev.ylp.notes.cli

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.terminal.YesNoPrompt
import dev.ylp.client.SharedClient
import dev.ylp.client.auth.application.DeviceSignInResult
import dev.ylp.client.auth.application.SignInResult
import dev.ylp.client.auth.domain.DeviceAuthorization
import dev.ylp.client.auth.domain.DeviceCodeDisplay
import dev.ylp.client.notes.domain.ConflictKind
import dev.ylp.client.notes.domain.DeleteNoteResult
import dev.ylp.client.notes.domain.Note
import dev.ylp.client.notes.domain.NoteConflict
import dev.ylp.client.notes.domain.NoteConflictResult
import dev.ylp.client.notes.domain.NoteFailure
import dev.ylp.client.notes.domain.NotePageResult
import dev.ylp.client.notes.domain.NoteResult
import dev.ylp.client.notes.domain.Resolution
import dev.ylp.client.notes.domain.ResolutionResult
import dev.ylp.client.notes.domain.SyncState
import dev.ylp.client.sync.domain.SyncResult

/**
 * The `notes` command and its subcommands, over the shared client. The client's browser signs in
 * when the provider has no device flow.
 */
fun notesCli(client: SharedClient): SuspendingCliktCommand =
    Notes().subcommands(
        Login(client),
        Logout(client),
        ListNotes(client),
        Show(client),
        Add(client),
        Edit(client),
        Delete(client),
        Sync(client),
        Resolve(client),
    )

private class Notes : SuspendingCliktCommand(name = "notes") {
    override fun help(context: Context) = "Your notes, from the terminal. Works offline: changes leave once the API can be reached."

    override suspend fun run() = Unit
}

private class Login(
    private val client: SharedClient,
) : SuspendingCliktCommand() {
    override fun help(context: Context) = "Sign in, with a code to type in a browser, or in the browser directly."

    override suspend fun run() {
        val display =
            object : DeviceCodeDisplay {
                override suspend fun show(authorization: DeviceAuthorization) {
                    echo(
                        "Open ${authorization.verificationUriComplete ?: authorization.verificationUri} and enter the code ${authorization.userCode}.",
                    )
                    echo("Waiting for the sign-in...")
                }
            }
        when (val device = client.signInOnDevice.execute(display)) {
            DeviceSignInResult.SignedIn -> {
                echo("Signed in.")
            }

            is DeviceSignInResult.Failed -> {
                fail(device.message)
            }

            DeviceSignInResult.Unsupported -> {
                when (val result = client.signIn.execute()) {
                    SignInResult.SignedIn -> echo("Signed in.")
                    SignInResult.Cancelled -> fail("Sign-in cancelled.")
                    is SignInResult.Failed -> fail(result.message)
                }
            }
        }
    }
}

private class Logout(
    private val client: SharedClient,
) : SuspendingCliktCommand() {
    private val force by option("--force", help = "Sign out even if changes are not synced yet").flag()

    override fun help(context: Context) = "Sign out, and forget the notes kept on this computer."

    override suspend fun run() {
        val pending = client.countPendingChanges.execute()
        if (pending > 0 &&
            !force
        ) {
            fail("Changes not synced yet: $pending. Signing out loses them: run notes logout --force to do it anyway.")
        }
        client.signOut.execute()
        echo("Signed out.")
    }
}

private class ListNotes(
    private val client: SharedClient,
) : SuspendingCliktCommand(name = "list") {
    private val page by option("--page", help = "The page to show, from 0").int().default(0)

    override fun help(context: Context) = "List your notes, most recently updated first."

    override suspend fun run() {
        when (val result = client.listNotes.execute(page, dev.ylp.client.notes.application.ListNotes.DEFAULT_PAGE_SIZE)) {
            is NotePageResult.Success -> {
                val page = result.page
                if (page.offline) echo(OFFLINE)
                echo("Your notes (${page.totalNotes})")
                if (page.notes.isEmpty()) echo("No notes yet.")
                page.notes.forEach { echo(it.line()) }
                if (page.hasNext) echo("More: notes list --page ${page.page + 1}")
            }

            is NotePageResult.Failed -> {
                fail(result.failure)
            }
        }
    }
}

private class Show(
    private val client: SharedClient,
) : SuspendingCliktCommand() {
    private val id by argument()

    override fun help(context: Context) = "Show a note, and both sides of a conflict."

    override suspend fun run() {
        val note = note(client.getNote.execute(id))
        echo(note.title)
        if (note.content.isNotEmpty()) echo(note.content)
        echo("id ${note.id}, version ${note.version}${note.sync.label()?.let { ", $it" }.orEmpty()}")
        if (note.sync == SyncState.CONFLICTING) {
            val conflict = client.getConflict.execute(id)
            if (conflict is NoteConflictResult.Found) echo(conflict.conflict.describe())
        }
    }
}

private class Add(
    private val client: SharedClient,
) : SuspendingCliktCommand() {
    private val title by option("--title").required()
    private val content by option("--content").default("")

    override fun help(context: Context) = "Write a new note."

    override suspend fun run() {
        val note = note(client.createNote.execute(title, content))
        echo("Added ${note.id}.${note.sync.after()}")
    }
}

private class Edit(
    private val client: SharedClient,
) : SuspendingCliktCommand() {
    private val id by argument()
    private val title by option("--title")
    private val content by option("--content")

    override fun help(context: Context) = "Change the title or the content of a note."

    override suspend fun run() {
        if (title == null && content == null) fail("Nothing to change: give --title, --content or both.")
        val current = note(client.getNote.execute(id))
        val note = note(client.updateNote.execute(id, title ?: current.title, content ?: current.content))
        echo("Saved.${note.sync.after()}")
    }
}

private class Delete(
    private val client: SharedClient,
) : SuspendingCliktCommand() {
    private val id by argument()
    private val yes by option("--yes", help = "Delete without asking").flag()

    override fun help(context: Context) = "Delete a note."

    override suspend fun run() {
        val note = note(client.getNote.execute(id))
        if (!yes && YesNoPrompt("Delete \"${note.title}\"?", terminal, default = false).ask() != true) {
            echo("Kept.")
            return
        }
        when (val result = client.deleteNote.execute(id)) {
            DeleteNoteResult.Deleted -> {
                val conflict = client.getConflict.execute(id)
                if (conflict is NoteConflictResult.Found) echo(conflict.conflict.describe()) else echo("Deleted.")
            }

            is DeleteNoteResult.Failed -> {
                fail(result.failure)
            }
        }
    }
}

private class Sync(
    private val client: SharedClient,
) : SuspendingCliktCommand() {
    override fun help(context: Context) = "Send the changes made offline."

    override suspend fun run() {
        when (client.syncChanges.execute()) {
            SyncResult.SYNCED -> {
                // What is left after a sync that went through waits for the user.
                val conflicts = client.countPendingChanges.execute()
                echo(if (conflicts == 0L) "Synced." else "Synced. Changes in conflict: $conflicts, see notes list.")
            }

            SyncResult.OFFLINE -> {
                fail("The service cannot be reached: changes not synced yet: ${client.countPendingChanges.execute()}.")
            }

            SyncResult.SIGNED_OUT -> {
                fail(NoteFailure.SignedOut)
            }
        }
    }
}

private class Resolve(
    private val client: SharedClient,
) : SuspendingCliktCommand() {
    private val id by argument()
    private val choice by argument(help = "mine, theirs or both").choice(
        "mine" to Resolution.KEEP_MINE,
        "theirs" to Resolution.KEEP_THEIRS,
        "both" to Resolution.KEEP_BOTH,
    )

    override fun help(context: Context) = "Resolve the conflict of a note changed elsewhere."

    override suspend fun run() {
        when (val result = client.resolveConflict.execute(id, choice)) {
            ResolutionResult.Resolved -> echo("Resolved.")
            is ResolutionResult.Failed -> fail(result.failure)
        }
    }
}

private fun note(result: NoteResult): Note =
    when (result) {
        is NoteResult.Success -> result.note
        is NoteResult.Failed -> fail(result.failure)
    }

private fun fail(failure: NoteFailure): Nothing = fail(failure.message())

private fun fail(message: String): Nothing = throw CliktError(message, statusCode = 1)

/** What a user reads for a failure. */
internal fun NoteFailure.message(): String =
    when (this) {
        NoteFailure.NotFound -> "This note does not exist, or it is not yours."
        is NoteFailure.Invalid -> violations.joinToString("\n") { listOfNotNull(it.field, it.message).joinToString(": ") }
        NoteFailure.SignedOut -> "Not signed in: run notes login."
        NoteFailure.Changed -> "This note was changed elsewhere."
        is NoteFailure.Unavailable -> "The service cannot be reached. Try again."
    }

private fun Note.line() = "$id  $title${sync.label()?.let { "  [$it]" }.orEmpty()}"

private fun SyncState.label(): String? =
    when (this) {
        SyncState.SYNCED -> null
        SyncState.PENDING -> "not synced"
        SyncState.CONFLICTING -> "conflict"
    }

// What a change became, after its first sync attempt.
private fun SyncState.after(): String =
    when (this) {
        SyncState.SYNCED -> ""
        SyncState.PENDING -> " Not synced yet: it leaves once the service can be reached."
        SyncState.CONFLICTING -> " It conflicts with a change made elsewhere: see notes show."
    }

private fun NoteConflict.describe(): String =
    buildString {
        appendLine(
            when (kind) {
                ConflictKind.CHANGED_ELSEWHERE -> {
                    if (mineDeleted) "You deleted this note, and it was changed elsewhere." else "This note was changed elsewhere."
                }

                ConflictKind.DELETED_ELSEWHERE -> {
                    "This note was deleted elsewhere."
                }

                ConflictKind.REJECTED -> {
                    "The service refused this change."
                }
            },
        )
        if (!mineDeleted) appendLine("Yours: ${mine.title}")
        theirs?.let { appendLine("Theirs: ${it.title}") }
        val choices = Resolution.entries.filter(::allows).joinToString(", ") { it.word() }
        append("Resolve with: notes resolve ${mine.id} <$choices>")
    }

private fun Resolution.word() =
    when (this) {
        Resolution.KEEP_MINE -> "mine"
        Resolution.KEEP_THEIRS -> "theirs"
        Resolution.KEEP_BOTH -> "both"
    }

/** What `list` says when the API cannot be reached. */
const val OFFLINE = "Offline: these are the notes kept on this computer."
