package dev.ylp.notes.ui

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import dev.ylp.client.auth.domain.Tokens
import dev.ylp.notes.FakeBackend
import dev.ylp.notes.MainActivity
import dev.ylp.notes.TestNotesApplication
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The notes flow on the real screens and the real shared client, against a mock backend. */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestNotesApplication::class)
class NotesFlowTest {
    // Ready before the rule launches the activity, which happens before any @Before method.
    private val backend = TestNotesApplication.reset()

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @After
    fun stopBackend() {
        backend.server.close()
    }

    @Test
    fun signingInShowsTheUsersNotes() {
        backend.add("Groceries", "Milk, eggs")

        signIn()

        waitFor("Groceries")
        waitFor("Your notes (1)")
    }

    @Test
    fun closingTheSignInPageCancelsTheSignIn() {
        TestNotesApplication.userCancels = true

        compose.onNodeWithText("Sign in").performClick()

        waitFor("Sign-in cancelled.")
    }

    @Test
    fun aNewNoteIsWrittenAndListed() {
        signIn()
        waitFor("No notes yet.")

        compose.onNodeWithText("New note").performClick()
        compose.onNodeWithText("Title").performTextInput("Groceries")
        compose.onNodeWithText("Content").performTextInput("Milk")
        compose.onNodeWithText("Add note").performClick()

        waitFor("Your notes (1)")
        waitFor("Groceries")
        assertEquals("Groceries" to "Milk", backend.notes.values.single())
    }

    @Test
    fun anEmptyTitleIsRejectedWithoutCallingTheApi() {
        signIn()
        waitFor("No notes yet.")

        compose.onNodeWithText("New note").performClick()
        compose.onNodeWithText("Add note").performClick()

        waitFor("title: must not be empty")
        assertFalse(backend.requests.contains("POST /api/v1/notes"))
    }

    @Test
    fun aNoteIsEdited() {
        backend.add("Groceries", "Milk")
        signIn()

        compose.waitUntilText("Groceries")
        compose.onNodeWithText("Groceries").performClick()
        compose.waitUntilText("Edit note")
        compose.waitUntilText("Milk")
        compose.onNodeWithText("Groceries").performTextClearance()
        compose.onNodeWithText("Title").performTextInput("Weekend groceries")
        compose.onNodeWithText("Save").performClick()

        waitFor("Your notes (1)")
        waitFor("Weekend groceries")
        assertEquals("Weekend groceries" to "Milk", backend.notes.values.single())
    }

    @Test
    fun aNoteChangedElsewhereIsKeptAsMine() {
        val id = changeOnBothSides()

        compose.onNodeWithText("Keep mine").performScrollTo().performClick()

        waitFor("Your notes (1)")
        waitFor("From the phone")
        assertEquals("From the phone" to "Milk", backend.notes.getValue(id))
    }

    @Test
    fun aNoteChangedElsewhereIsKeptAsTheirs() {
        val id = changeOnBothSides()

        compose.onNodeWithText("Keep theirs").performScrollTo().performClick()

        waitFor("Your notes (1)")
        waitFor("From the browser")
        assertEquals("From the browser" to "Eggs", backend.notes.getValue(id))
    }

    @Test
    fun aNoteChangedElsewhereIsKeptOnBothSides() {
        changeOnBothSides()

        compose.onNodeWithText("Keep both").performScrollTo().performClick()

        waitFor("Your notes (2)")
        assertEquals(setOf("From the phone", "From the browser"), backend.notes.values.map { it.first }.toSet())
    }

    @Test
    fun aNoteWrittenOfflineIsListedThenSentWhenTheApiIsBack() {
        backend.add("Groceries")
        signIn()
        waitFor("Groceries")
        backend.offline = true

        compose.onNodeWithText("New note").performClick()
        compose.onNodeWithText("Title").performTextInput("Books")
        compose.onNodeWithText("Add note").performClick()

        waitFor(OFFLINE)
        waitFor("Books")
        waitFor("Not synced")
        backend.offline = false
        compose.onNodeWithText("Retry").performClick()
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText("Not synced").fetchSemanticsNodes().isEmpty() }
        assertEquals(setOf("Groceries", "Books"), backend.notes.values.map { it.first }.toSet())
    }

    @Test
    fun signingOutWithChangesNotSyncedAsksFirst() {
        signIn()
        waitFor("No notes yet.")
        backend.offline = true
        compose.onNodeWithText("New note").performClick()
        compose.onNodeWithText("Title").performTextInput("Books")
        compose.onNodeWithText("Add note").performClick()
        waitFor("Not synced")

        compose.onNodeWithText("Sign out").performClick()
        waitFor("Changes not synced yet: 1. Signing out loses them.")
        compose.onNodeWithText("Stay").performClick()
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText("Stay").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Sign out").performClick()
        waitFor("Sign out anyway")
        compose.onNodeWithText("Sign out anyway").performClick()

        waitFor("Sign in")
    }

    @Test
    fun aNoteIsDeletedOnlyOnceConfirmed() {
        backend.add("Groceries")
        signIn()
        waitFor("Groceries")
        compose.onNodeWithText("Groceries").performClick()
        waitFor("Edit note")

        compose.onNodeWithText("Delete").performClick()
        compose.onNodeWithText("Keep").performClick()
        assertEquals(1, backend.notes.size)
        compose.onNodeWithText("Delete").performClick()
        compose.onNodeWithText("Delete this note?").assertExists()
        compose.onAllNodesWithText("Delete").onLast().performClick()

        waitFor("No notes yet.")
        assertEquals(0, backend.notes.size)
    }

    @Test
    fun aNoteGoneMeanwhileIsReportedAsNotFound() {
        val id = backend.add("Groceries")
        signIn()
        waitFor("Groceries")
        backend.notes.remove(id)

        compose.onNodeWithText("Groceries").performClick()

        waitFor("This note does not exist, or it is not yours.")
    }

    @Test
    fun moreNotesLoadOnRequest() {
        repeat(25) { backend.add("Note $it") }
        signIn()
        waitFor("Your notes (25)")

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Load more"))
        compose.onNodeWithText("Load more").performClick()

        compose.waitUntil(TIMEOUT) { runCatching { compose.onNode(hasScrollAction()).performScrollToNode(hasText("Note 24")) }.isSuccess }
    }

    @Test
    fun anUnreachableApiCanBeRetried() {
        backend.add("Groceries")
        backend.failNextWith = 503
        signIn()

        waitFor(OFFLINE)
        compose.onNodeWithText("Retry").performClick()

        waitFor("Groceries")
    }

    @Test
    fun anEndedSessionLeadsBackToSignIn() {
        backend.refreshAllowed = false
        signIn()
        waitFor("No notes yet.")
        runBlocking { TestNotesApplication.tokens.save(Tokens("expired-token", "refresh", null)) }

        compose.onNodeWithText("New note").performClick()
        compose.onNodeWithText("Title").performTextInput("Groceries")
        compose.onNodeWithText("Add note").performClick()

        waitFor("Sign in")
        assertEquals(null, runBlocking { TestNotesApplication.tokens.load() })
        assertEquals(0, backend.notes.size)
    }

    @Test
    fun signingOutLeadsBackToSignIn() {
        signIn()
        waitFor("No notes yet.")

        compose.onNodeWithText("Sign out").performClick()

        waitFor("Sign in")
        assertEquals(null, runBlocking { TestNotesApplication.tokens.load() })
    }

    /** A note opened here, changed elsewhere, then saved here: the editor shows the conflict. */
    private fun changeOnBothSides(): String {
        val id = backend.add("Groceries", "Milk")
        signIn()
        compose.waitUntilText("Groceries")
        compose.onNodeWithText("Groceries").performClick()
        compose.waitUntilText("Milk")
        backend.changeElsewhere(id, "From the browser", "Eggs")

        compose.onNodeWithText("Groceries").performTextClearance()
        compose.onNodeWithText("Title").performTextInput("From the phone")
        compose.onNodeWithText("Save").performClick()

        waitFor("This note was changed elsewhere.")
        waitFor("From the browser")
        assertEquals("From the browser" to "Eggs", backend.notes.getValue(id))
        return id
    }

    private fun signIn() {
        waitFor("Sign in")
        compose.onNodeWithText("Sign in").performClick()
    }

    private fun waitFor(text: String) = compose.waitUntilText(text)

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
