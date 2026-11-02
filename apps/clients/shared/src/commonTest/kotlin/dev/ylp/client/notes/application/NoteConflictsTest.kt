package dev.ylp.client.notes.application

import dev.ylp.client.notes.domain.ConflictKind
import dev.ylp.client.notes.domain.NoteConflict
import dev.ylp.client.notes.domain.NoteConflictResult
import dev.ylp.client.notes.domain.NoteFailure
import dev.ylp.client.notes.domain.Resolution
import dev.ylp.client.notes.domain.ResolutionResult
import dev.ylp.client.notes.domain.SyncState
import dev.ylp.client.sync.domain.SyncResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The notes strategy: a change refused by the API stays, next to the API's state, until resolved. */
class NoteConflictsTest : NotesFixture() {
    private suspend fun conflict(id: String): NoteConflict = (getConflict.execute(id) as NoteConflictResult.Found).conflict

    /** A note changed here offline and elsewhere meanwhile, synced: it is in conflict. */
    private suspend fun changedOnBothSides(): String {
        val note = cached("Groceries")
        api.offline = true
        updateNote.execute(note.id, "Mine", "")
        api.changeElsewhere(note.id, "Theirs")
        api.offline = false
        sync.execute()
        return note.id
    }

    @Test
    fun `a change on an outdated version is kept in conflict and the API's note is untouched`() =
        runTest {
            val id = changedOnBothSides()

            val conflict = conflict(id)
            assertEquals(ConflictKind.CHANGED_ELSEWHERE, conflict.kind)
            assertEquals("Mine", conflict.mine.title)
            assertEquals("Theirs", conflict.theirs?.title)
            assertEquals(id, conflict.theirs?.id)
            assertEquals(listOf(Resolution.KEEP_MINE, Resolution.KEEP_THEIRS, Resolution.KEEP_BOTH), conflict.resolutions)
            assertEquals("Theirs", api.notes.getValue(id).title)
            assertEquals(SyncState.CONFLICTING, getNote.execute(id).note().sync)
            assertEquals(listOf("Mine"), titles())
        }

    @Test
    fun `a conflict found online comes back from the change at once`() =
        runTest {
            val note = cached("Groceries")
            api.changeElsewhere(note.id, "Theirs")

            assertEquals(SyncState.CONFLICTING, updateNote.execute(note.id, "Mine", "").note().sync)
        }

    @Test
    fun `keeping mine sends it again on top of the latest version`() =
        runTest {
            val id = changedOnBothSides()

            assertEquals(ResolutionResult.Resolved, resolveConflict.execute(id, Resolution.KEEP_MINE))

            assertEquals("Mine", api.notes.getValue(id).title)
            assertEquals("update s1 Mine at 1", api.calls.last())
            assertEquals(SyncState.SYNCED, getNote.execute(id).note().sync)
            assertEquals(0, pending.execute())
        }

    @Test
    fun `keeping theirs drops mine`() =
        runTest {
            val id = changedOnBothSides()

            resolveConflict.execute(id, Resolution.KEEP_THEIRS)

            assertEquals(listOf("Theirs"), titles())
            assertEquals(0, pending.execute())
            assertEquals(NoteConflictResult.Failed(NoteFailure.NotFound), getConflict.execute(id))
        }

    @Test
    fun `keeping both writes mine as a new note`() =
        runTest {
            val id = changedOnBothSides()

            resolveConflict.execute(id, Resolution.KEEP_BOTH)

            assertEquals(
                setOf("Mine", "Theirs"),
                api.notes.values
                    .map { it.title }
                    .toSet(),
            )
            assertEquals(setOf("Mine", "Theirs"), titles().toSet())
            assertEquals(0, pending.execute())
        }

    @Test
    fun `a note changed here and deleted elsewhere can be written again`() =
        runTest {
            val note = cached("Groceries")
            api.offline = true
            updateNote.execute(note.id, "Mine", "")
            api.deleteElsewhere(note.id)
            api.offline = false
            sync.execute()
            val conflict = conflict(note.id)
            assertEquals(ConflictKind.DELETED_ELSEWHERE, conflict.kind)
            assertNull(conflict.theirs)
            assertEquals(listOf(Resolution.KEEP_MINE, Resolution.KEEP_THEIRS), conflict.resolutions)

            resolveConflict.execute(note.id, Resolution.KEEP_MINE)

            assertEquals(
                "Mine",
                api.notes.values
                    .single()
                    .title,
            )
            assertEquals(SyncState.SYNCED, getNote.execute(note.id).note().sync)
        }

    @Test
    fun `a note changed here and deleted elsewhere can be let go`() =
        runTest {
            val note = cached("Groceries")
            api.deleteElsewhere(note.id)

            updateNote.execute(note.id, "Mine", "")
            resolveConflict.execute(note.id, Resolution.KEEP_THEIRS)

            assertEquals(emptyList(), titles())
            assertTrue(api.notes.isEmpty())
        }

    @Test
    fun `a deletion of a note changed elsewhere waits for the user`() =
        runTest {
            val note = cached("Groceries")
            api.changeElsewhere(note.id, "Theirs")

            deleteNote.execute(note.id)

            val conflict = conflict(note.id)
            assertTrue(conflict.mineDeleted)
            assertEquals(listOf(Resolution.KEEP_MINE, Resolution.KEEP_THEIRS), conflict.resolutions)
            assertEquals("Theirs", api.notes.getValue(note.id).title)
            resolveConflict.execute(note.id, Resolution.KEEP_MINE)
            assertTrue(api.notes.isEmpty())
        }

    @Test
    fun `a deletion let go brings the note back`() =
        runTest {
            val note = cached("Groceries")
            api.changeElsewhere(note.id, "Theirs")
            deleteNote.execute(note.id)

            resolveConflict.execute(note.id, Resolution.KEEP_THEIRS)

            assertEquals(listOf("Theirs"), titles())
        }

    @Test
    fun `a change the API rejects can only be dropped`() =
        runTest {
            val note = createNote.execute(FakeNoteApi.REJECTED_TITLE, "").note()
            assertEquals(SyncState.CONFLICTING, note.sync)
            assertEquals(listOf(Resolution.KEEP_THEIRS), conflict(note.id).resolutions)

            val refused = resolveConflict.execute(note.id, Resolution.KEEP_MINE)

            assertIs<ResolutionResult.Failed>(refused)
            resolveConflict.execute(note.id, Resolution.KEEP_THEIRS)
            assertEquals(emptyList(), titles())
            assertEquals(0, pending.execute())
        }

    @Test
    fun `a conflict holds back its own note only`() =
        runTest {
            val id = changedOnBothSides()
            api.offline = true
            createNote.execute("Other", "")
            api.offline = false

            assertEquals(SyncResult.SYNCED, sync.execute())

            assertTrue(api.notes.values.any { it.title == "Other" })
            assertEquals(1, pending.execute())
            assertEquals(SyncState.CONFLICTING, getNote.execute(id).note().sync)
        }

    @Test
    fun `a conflicting note changed again stays in conflict with the new change`() =
        runTest {
            val id = changedOnBothSides()

            updateNote.execute(id, "Mine, again", "")

            assertEquals("Mine, again", conflict(id).mine.title)
            assertEquals(SyncState.CONFLICTING, getNote.execute(id).note().sync)
        }

    @Test
    fun `a note without conflict has nothing to resolve`() =
        runTest {
            val note = cached("Groceries")

            assertEquals(NoteConflictResult.Failed(NoteFailure.NotFound), getConflict.execute(note.id))
            assertEquals(ResolutionResult.Failed(NoteFailure.NotFound), resolveConflict.execute(note.id, Resolution.KEEP_MINE))
            assertFalse(api.calls.any { it.startsWith("update") })
        }
}
