package dev.ylp.client.notes.application

import dev.ylp.client.notes.domain.DeleteNoteResult
import dev.ylp.client.notes.domain.NoteFailure
import dev.ylp.client.notes.domain.NotePageResult
import dev.ylp.client.notes.domain.NoteResult
import dev.ylp.client.notes.domain.SyncState
import dev.ylp.client.notes.domain.Violation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The use cases with the API reachable: each change reaches it at once. */
class NoteUseCasesTest : NotesFixture() {
    @Test
    fun `a valid note is written to the API`() =
        runTest {
            val note = createNote.execute("Groceries", "Milk").note()

            assertEquals(SyncState.SYNCED, note.sync)
            assertEquals("s1", note.id)
            assertEquals(listOf("create Groceries"), api.calls)
            assertEquals(
                "Milk",
                api.notes.values
                    .single()
                    .content,
            )
        }

    @Test
    fun `an empty title is rejected without calling the API`() =
        runTest {
            val result = createNote.execute("", "Milk")

            assertEquals(NoteResult.Failed(NoteFailure.Invalid(listOf(Violation("title", "must not be empty")))), result)
            assertTrue(api.calls.isEmpty())
        }

    @Test
    fun `a title or content over the contract's limits is rejected`() =
        runTest {
            val result = updateNote.execute("id", "t".repeat(201), "c".repeat(20_001))

            val failure = (result as NoteResult.Failed).failure as NoteFailure.Invalid
            assertEquals(listOf("title", "content"), failure.violations.map { it.field })
            assertTrue(api.calls.isEmpty())
        }

    @Test
    fun `a change is sent with the version it is based on`() =
        runTest {
            val note = cached("Groceries")
            api.changeElsewhere(note.id, "Shopping")
            getNote.execute(note.id)

            val updated = updateNote.execute(note.id, "Food", "Milk").note()

            assertEquals("update s1 Food at 1", api.calls.last())
            assertEquals(2, updated.version)
            assertEquals(SyncState.SYNCED, updated.sync)
        }

    @Test
    fun `notes are listed by page most recent first`() =
        runTest {
            repeat(3) { api.add("Note $it") }

            val result = listNotes.execute(page = 1, size = 2) as NotePageResult.Success

            assertEquals(listOf("Note 0"), result.page.notes.map { it.title })
            assertEquals(3, result.page.totalNotes)
            assertFalse(result.page.hasNext)
            assertFalse(result.page.offline)
        }

    @Test
    fun `a page out of range is rejected`() =
        runTest {
            val result = listNotes.execute(page = -1, size = 101)

            val failure = (result as NotePageResult.Failed).failure as NoteFailure.Invalid
            assertEquals(listOf("page", "size"), failure.violations.map { it.field })
            assertTrue(api.calls.isEmpty())
        }

    @Test
    fun `an empty page is rejected`() =
        runTest {
            val result = listNotes.execute(page = 0, size = 0)

            assertEquals(NotePageResult.Failed(NoteFailure.Invalid(listOf(Violation("size", "must be between 1 and 100")))), result)
        }

    @Test
    fun `a note is read then deleted`() =
        runTest {
            val note = api.add("Groceries")

            assertEquals(NoteResult.Success(note), getNote.execute(note.id))
            assertEquals(DeleteNoteResult.Deleted, deleteNote.execute(note.id))
            assertEquals(listOf("get s1", "delete s1 at 0"), api.calls)
            assertTrue(api.notes.isEmpty())
            assertEquals(NoteResult.Failed(NoteFailure.NotFound), getNote.execute(note.id))
        }

    @Test
    fun `an unknown note can be neither changed nor deleted`() =
        runTest {
            assertEquals(NoteResult.Failed(NoteFailure.NotFound), updateNote.execute("unknown", "Title", ""))
            assertEquals(DeleteNoteResult.Failed(NoteFailure.NotFound), deleteNote.execute("unknown"))
        }

    @Test
    fun `a note deleted elsewhere is forgotten when read`() =
        runTest {
            val note = cached("Groceries")
            api.deleteElsewhere(note.id)

            assertEquals(NoteResult.Failed(NoteFailure.NotFound), getNote.execute(note.id))
            api.offline = true
            assertEquals(NoteResult.Failed(NoteFailure.Unavailable("offline")), getNote.execute(note.id))
        }

    @Test
    fun `notes deleted elsewhere leave the list`() =
        runTest {
            val kept = cached("Kept")
            val gone = cached("Gone")
            api.deleteElsewhere(gone.id)

            assertEquals(listOf(kept.title), titles())
            api.offline = true
            assertEquals(listOf(kept.title), titles())
        }

    @Test
    fun `an ended session stops the listing`() =
        runTest {
            api.signedOut = true

            assertEquals(NotePageResult.Failed(NoteFailure.SignedOut), listNotes.execute())
        }
}
