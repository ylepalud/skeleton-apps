package dev.ylp.client.notes.application

import dev.ylp.client.notes.domain.DeleteNoteResult
import dev.ylp.client.notes.domain.NoteResult
import dev.ylp.client.notes.domain.SyncState
import dev.ylp.client.sync.domain.SyncResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Without the API: reads come from this device, changes wait in the queue and replay in order. */
class OfflineNotesTest : NotesFixture() {
    @Test
    fun `the notes read before are listed offline`() =
        runTest {
            cached("Groceries")
            cached("Books")
            api.offline = true

            val page = page()

            assertTrue(page.offline)
            assertEquals(listOf("Books", "Groceries"), page.notes.map { it.title })
            assertEquals(2, page.totalNotes)
        }

    @Test
    fun `an offline page counts what is beyond it`() =
        runTest {
            repeat(3) { api.add("Note $it") }
            listNotes.execute()
            api.offline = true

            val page = (listNotes.execute(page = 0, size = 2) as dev.ylp.client.notes.domain.NotePageResult.Success).page

            assertEquals(listOf("Note 2", "Note 1"), page.notes.map { it.title })
            assertTrue(page.hasNext)
        }

    @Test
    fun `a note read before is read offline`() =
        runTest {
            val note = cached("Groceries")
            api.offline = true

            assertEquals(NoteResult.Success(note), getNote.execute(note.id))
        }

    @Test
    fun `a note written offline waits then shows under its API id which its local id still finds`() =
        runTest {
            api.offline = true
            val note = createNote.execute("Groceries", "Milk").note()
            assertEquals(SyncState.PENDING, note.sync)
            assertEquals("local-1", note.id)
            assertEquals(listOf("Groceries"), titles())
            assertEquals(1, pending.execute())

            api.offline = false
            assertEquals(SyncResult.SYNCED, sync.execute())

            assertEquals(
                "Groceries",
                api.notes.values
                    .single()
                    .title,
            )
            assertEquals(0, pending.execute())
            assertEquals(listOf("s1"), page().notes.map { it.id })
            val synced = getNote.execute(note.id).note()
            assertEquals("s1", synced.id)
            assertEquals(SyncState.SYNCED, synced.sync)
            assertEquals(SyncState.SYNCED, updateNote.execute("s1", "Food", "").note().sync)
            assertEquals("update s1 Food at 0", api.calls.last())
        }

    @Test
    fun `changes replay in the order they were made each with the version it is based on`() =
        runTest {
            val first = cached("First")
            val second = cached("Second")
            api.offline = true
            updateNote.execute(second.id, "Second, changed", "")
            updateNote.execute(first.id, "First, changed", "")
            deleteNote.execute(second.id)
            createNote.execute("Third", "")
            api.offline = false
            api.calls.clear()

            sync.execute()

            assertEquals(listOf("delete s2 at 0", "update s1 First, changed at 0", "create Third"), api.calls)
        }

    @Test
    fun `a note written then changed offline is sent once as last written`() =
        runTest {
            api.offline = true
            val note = createNote.execute("Draft", "").note()
            updateNote.execute(note.id, "Groceries", "Milk")
            api.offline = false
            api.calls.clear()

            sync.execute()

            assertEquals(listOf("create Groceries"), api.calls)
        }

    @Test
    fun `a note written then deleted offline never reaches the API`() =
        runTest {
            api.offline = true
            val note = createNote.execute("Draft", "").note()

            assertEquals(DeleteNoteResult.Deleted, deleteNote.execute(note.id))
            assertEquals(0, pending.execute())
            assertEquals(emptyList(), titles())
        }

    @Test
    fun `a note changed twice offline is sent once on the version first read`() =
        runTest {
            val note = cached("Groceries")
            api.offline = true
            updateNote.execute(note.id, "Shopping", "")
            updateNote.execute(note.id, "Food", "")
            api.offline = false
            api.calls.clear()

            sync.execute()

            assertEquals(listOf("update s1 Food at 0"), api.calls)
        }

    @Test
    fun `a note deleted offline is hidden then deleted on the API`() =
        runTest {
            val note = cached("Groceries")
            api.offline = true
            deleteNote.execute(note.id)

            assertEquals(emptyList(), titles())
            assertEquals(NoteResult.Failed(dev.ylp.client.notes.domain.NoteFailure.NotFound), getNote.execute(note.id))
            api.offline = false
            assertEquals(emptyList(), titles())
            assertTrue(api.notes.isEmpty())
        }

    @Test
    fun `a note deleted offline and elsewhere is simply gone`() =
        runTest {
            val note = cached("Groceries")
            api.offline = true
            deleteNote.execute(note.id)
            api.deleteElsewhere(note.id)
            api.offline = false

            assertEquals(SyncResult.SYNCED, sync.execute())
            assertEquals(0, pending.execute())
        }

    @Test
    fun `a note changed offline shows the change until the API has it`() =
        runTest {
            val note = cached("Groceries")
            api.offline = true
            updateNote.execute(note.id, "Shopping", "")

            assertEquals(SyncState.PENDING, getNote.execute(note.id).note().sync)
            api.offline = false
            assertEquals(listOf("Shopping"), titles())
            assertEquals(SyncState.SYNCED, getNote.execute(note.id).note().sync)
        }

    @Test
    fun `the notes the API does not have yet come first in the list`() =
        runTest {
            cached("From the API")
            createNote.execute(FakeNoteApi.REJECTED_TITLE, "")

            val page = page()

            assertEquals(listOf(FakeNoteApi.REJECTED_TITLE, "From the API"), page.notes.map { it.title })
            assertEquals(2, page.totalNotes)
        }

    @Test
    fun `an ended session stops the replay and keeps the changes`() =
        runTest {
            api.offline = true
            createNote.execute("Groceries", "")
            api.offline = false
            api.signedOut = true

            assertEquals(SyncResult.SIGNED_OUT, sync.execute())
            assertEquals(1, pending.execute())
        }

    @Test
    fun `the replay stops at the first change the API cannot receive`() =
        runTest {
            api.offline = true
            createNote.execute("First", "")
            createNote.execute("Second", "")
            api.calls.clear()

            assertEquals(SyncResult.OFFLINE, sync.execute())
            assertEquals(listOf("create First"), api.calls)
            assertEquals(2, pending.execute())
        }
}
