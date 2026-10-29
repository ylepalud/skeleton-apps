package dev.ylp.client.notes.application

import dev.ylp.client.notes.domain.DeleteNoteResult
import dev.ylp.client.notes.domain.NoteFailure
import dev.ylp.client.notes.domain.NotePageResult
import dev.ylp.client.notes.domain.NoteResult
import dev.ylp.client.notes.domain.Violation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NoteUseCasesTest {
    private val gateway = FakeNoteGateway()

    @Test
    fun `a valid note is written`() =
        runTest {
            val result = CreateNote(gateway).execute("Groceries", "Milk")

            assertEquals(NoteResult.Success(FakeNoteGateway.NOTE), result)
            assertEquals(listOf("create Groceries"), gateway.calls)
        }

    @Test
    fun `an empty title is rejected without calling the API`() =
        runTest {
            val result = CreateNote(gateway).execute("", "Milk")

            assertEquals(NoteResult.Failed(NoteFailure.Invalid(listOf(Violation("title", "must not be empty")))), result)
            assertTrue(gateway.calls.isEmpty())
        }

    @Test
    fun `a title or content over the contract's limits is rejected`() =
        runTest {
            val result = UpdateNote(gateway).execute("id", "t".repeat(201), "c".repeat(20_001))

            val failure = (result as NoteResult.Failed).failure as NoteFailure.Invalid
            assertEquals(listOf("title", "content"), failure.violations.map { it.field })
            assertTrue(gateway.calls.isEmpty())
        }

    @Test
    fun `a valid replacement is sent`() =
        runTest {
            UpdateNote(gateway).execute("id", "Groceries", "Milk")

            assertEquals(listOf("update id Groceries"), gateway.calls)
        }

    @Test
    fun `notes are listed by page`() =
        runTest {
            val result = ListNotes(gateway).execute(page = 2, size = 10)

            assertTrue(result is NotePageResult.Success)
            assertEquals(listOf("list 2 10"), gateway.calls)
        }

    @Test
    fun `a page out of range is rejected`() =
        runTest {
            val result = ListNotes(gateway).execute(page = -1, size = 101)

            val failure = (result as NotePageResult.Failed).failure as NoteFailure.Invalid
            assertEquals(listOf("page", "size"), failure.violations.map { it.field })
            assertTrue(gateway.calls.isEmpty())
        }

    @Test
    fun `an empty page is rejected`() =
        runTest {
            val result = ListNotes(gateway).execute(page = 0, size = 0)

            assertEquals(NotePageResult.Failed(NoteFailure.Invalid(listOf(Violation("size", "must be between 1 and 100")))), result)
        }

    @Test
    fun `a note is read and deleted by id`() =
        runTest {
            assertEquals(NoteResult.Success(FakeNoteGateway.NOTE), GetNote(gateway).execute("id"))
            assertEquals(DeleteNoteResult.Deleted, DeleteNote(gateway).execute("id"))
            assertEquals(listOf("get id", "delete id"), gateway.calls)
        }
}
