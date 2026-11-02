package dev.ylp.client.notes.application

import dev.ylp.client.notes.adapter.out.local.SqlNoteStore
import dev.ylp.client.notes.domain.Note
import dev.ylp.client.notes.domain.NotePage
import dev.ylp.client.notes.domain.NotePageResult
import dev.ylp.client.notes.domain.NoteResult
import dev.ylp.client.sync.adapter.out.local.SqlPendingChanges
import dev.ylp.client.sync.application.CountPendingChanges
import dev.ylp.client.sync.application.SyncChanges
import dev.ylp.client.sync.application.SyncEngine
import dev.ylp.client.testStore
import kotlinx.coroutines.sync.Mutex
import kotlin.time.Clock
import kotlin.time.Instant

/** The note use cases over a real local store in memory and a [FakeNoteApi]. */
open class NotesFixture {
    protected val api = FakeNoteApi()
    private val database = testStore().database
    private val changes = SqlPendingChanges(database)
    private val lock = Mutex()
    private var lastId = 0
    private val clock =
        object : Clock {
            override fun now() = Instant.parse("2026-11-02T08:00:00Z")
        }
    private val notes = LocalNotes(SqlNoteStore(database), changes, api, lock, clock) { "local-${++lastId}" }
    private val engine = SyncEngine(changes, listOf(notes), lock)

    protected val listNotes = ListNotes(notes, api, engine)
    protected val getNote = GetNote(notes, api)
    protected val createNote = CreateNote(notes, engine)
    protected val updateNote = UpdateNote(notes, engine)
    protected val deleteNote = DeleteNote(notes, engine)
    protected val getConflict = GetConflict(notes)
    protected val resolveConflict = ResolveConflict(notes, engine)
    protected val sync = SyncChanges(engine)
    protected val pending = CountPendingChanges(changes)

    protected suspend fun page(): NotePage = (listNotes.execute() as NotePageResult.Success).page

    protected suspend fun titles(): List<String> = page().notes.map { it.title }

    protected fun NoteResult.note(): Note = (this as NoteResult.Success).note

    /** A note of the API, read once so that this device keeps it. */
    protected suspend fun cached(title: String): Note {
        val note = api.add(title)
        listNotes.execute()
        return note
    }
}
