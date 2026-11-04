package dev.ylp.notes.cli

import com.github.ajalt.clikt.command.parse
import com.github.ajalt.clikt.testing.test
import dev.ylp.client.SharedClient
import dev.ylp.client.auth.adapter.out.memory.InMemoryTokenStore
import dev.ylp.client.auth.domain.Tokens
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** The commands on the real shared client, against a fake backend over HTTP. */
class NotesCliTest {
    private val backend = FakeBackend()
    private val tokens = InMemoryTokenStore()
    private lateinit var client: SharedClient
    private val opened = mutableListOf<String>()

    @BeforeTest
    fun start() =
        runBlocking {
            backend.start()
            val port = 20_000 + Random.nextInt(20_000)
            val settings = Settings(backend.url("/api/v1"), backend.url("/idp"), "api-cli", port)
            // The user's browser: it opens the sign-in page, which redirects to the loopback at once.
            val browser =
                LoopbackBrowser(port, open = { url ->
                    opened += url
                    HttpClient().use { it.get(url) }
                })
            client = SharedClient(settings.apiBaseUrl, settings.auth(), browser, testLocalStore(), tokens)
        }

    @AfterTest
    fun stop() = runBlocking { backend.stop() }

    private fun signedIn() = runBlocking { tokens.save(Tokens(FakeBackend.TOKEN, "refresh", null)) }

    private fun notes(
        argv: String,
        stdin: String = "",
    ) = notesCli(client).let { command ->
        command.test(argv.split(" "), stdin = stdin) { args -> runBlocking { command.parse(args) } }
    }

    @Test
    fun signsInInTheBrowserWhenTheProviderHasNoDeviceFlow() {
        val result = notes("login")

        assertEquals(0, result.statusCode)
        assertContains(result.stdout, "Signed in.")
        assertContains(opened.single(), backend.url("/idp/authorize"))
        assertEquals(FakeBackend.TOKEN, runBlocking { tokens.load() }?.accessToken)
    }

    @Test
    fun signsInWithACodeWhenTheProviderHasTheDeviceFlow() {
        backend.deviceFlow = true

        val result = notes("login")

        assertContains(result.stdout, "Open ${backend.url("/idp/activate?user_code=WDJB-MJHT")} and enter the code WDJB-MJHT.")
        assertContains(result.stdout, "Signed in.")
        assertEquals(emptyList(), opened)
        assertNotNull(runBlocking { tokens.load() })
    }

    @Test
    fun aNoteIsAddedListedShownEditedThenDeleted() {
        signedIn()

        assertContains(notes("add --title Groceries --content Milk").stdout, "Added ")
        val list = notes("list").stdout
        assertContains(list, "Your notes (1)")
        assertContains(list, "Groceries")
        val id = list.lines().first { "Groceries" in it }.substringBefore(" ")
        assertContains(notes("show $id").stdout, "Milk")
        assertContains(notes("edit $id --title Shopping").stdout, "Saved.")
        assertEquals(FakeBackend.StoredNote("Shopping", "Milk", 1), backend.notes.values.single())
        assertContains(notes("delete $id --yes").stdout, "Deleted.")
        assertEquals(emptyMap(), backend.notes)
    }

    @Test
    fun aDeletionWaitsForAYes() {
        signedIn()
        backend.add("Groceries")

        assertContains(notes("delete n1", stdin = "n\n").stdout, "Kept.")
        assertEquals(1, backend.notes.size)
        assertContains(notes("delete n1", stdin = "y\n").stdout, "Deleted.")
        assertEquals(0, backend.notes.size)
    }

    @Test
    fun aNoteWrittenOfflineIsSentBySync() {
        signedIn()
        backend.offline = true

        assertContains(notes("add --title Groceries").stdout, "Not synced yet")
        val offline = notes("list").stdout
        assertContains(offline, OFFLINE)
        assertContains(offline, "[not synced]")
        val waiting = notes("sync")
        assertEquals(1, waiting.statusCode)
        assertContains(waiting.stderr, "changes not synced yet: 1")

        backend.offline = false
        assertContains(notes("sync").stdout, "Synced.")
        assertEquals(
            "Groceries",
            backend.notes.values
                .single()
                .title,
        )
    }

    @Test
    fun aConflictIsShownThenResolved() {
        signedIn()
        backend.add("Groceries")
        notes("list")
        backend.offline = true
        assertContains(notes("edit n1 --title Mine").stdout, "Not synced yet")
        backend.changeElsewhere("n1", "Theirs")
        backend.offline = false

        assertContains(notes("sync").stdout, "Changes in conflict: 1, see notes list.")
        assertContains(notes("list").stdout, "[conflict]")
        val shown = notes("show n1").stdout
        assertContains(shown, "This note was changed elsewhere.")
        assertContains(shown, "Theirs: Theirs")
        assertContains(shown, "notes resolve n1 <mine, theirs, both>")
        assertContains(notes("resolve n1 mine").stdout, "Resolved.")
        assertEquals("Mine", backend.notes.getValue("n1").title)
    }

    @Test
    fun aSignOutWithChangesNotSyncedNeedsForce() {
        signedIn()
        backend.offline = true
        notes("add --title Groceries")

        val refused = notes("logout")
        assertEquals(1, refused.statusCode)
        assertContains(refused.stderr, "Changes not synced yet: 1")
        assertContains(notes("logout --force").stdout, "Signed out.")
        assertNull(runBlocking { tokens.load() })
    }

    @Test
    fun aCommandWithoutSessionAsksToSignIn() {
        val result = notes("list")

        assertEquals(1, result.statusCode)
        assertContains(result.stderr, "Not signed in: run notes login.")
    }

    @Test
    fun anEditNeedsSomethingToChange() {
        signedIn()

        assertContains(notes("edit n1").stderr, "Nothing to change")
    }

    @Test
    fun anUnknownNoteIsNotFound() {
        signedIn()

        assertContains(notes("show n9").stderr, "This note does not exist, or it is not yours.")
    }

    @Test
    fun aSignInTheProviderRefusesFails() {
        backend.tokenError = "access_denied"

        val result = notes("login")

        assertEquals(1, result.statusCode)
        assertContains(result.stderr, "access_denied")
    }

    @Test
    fun aDeviceSignInTheUserRefusesFails() {
        backend.deviceFlow = true
        backend.tokenError = "access_denied"

        assertContains(notes("login").stderr, "The sign-in was refused")
    }

    @Test
    fun anEmptyListSaysSoAndALongOneHasMorePages() {
        signedIn()
        assertContains(notes("list").stdout, "No notes yet.")

        repeat(21) { backend.add("Note $it") }

        assertContains(notes("list").stdout, "More: notes list --page 1")
        assertContains(notes("list --page 1").stdout, "Note 0")
    }

    @Test
    fun onlyTheContentCanBeChanged() {
        signedIn()
        backend.add("Groceries")

        notes("edit n1 --content Eggs")

        assertEquals(FakeBackend.StoredNote("Groceries", "Eggs", 1), backend.notes.getValue("n1"))
    }

    @Test
    fun aDeletionOfANoteChangedElsewhereWaitsForTheUser() {
        signedIn()
        backend.add("Groceries")
        notes("list")
        backend.offline = true
        notes("delete n1 --yes")
        backend.changeElsewhere("n1", "Theirs")
        backend.offline = false
        notes("sync")

        val shown = notes("show n1").stdout
        assertContains(shown, "You deleted this note, and it was changed elsewhere.")
        assertContains(shown, "<mine, theirs>")
        assertContains(notes("resolve n1 theirs").stdout, "Resolved.")
        assertEquals("Theirs", backend.notes.getValue("n1").title)
    }

    @Test
    fun aNoteChangedHereAndDeletedElsewhereCanBeWrittenAgain() {
        signedIn()
        backend.add("Groceries")
        notes("list")
        backend.offline = true
        notes("edit n1 --title Mine")
        backend.notes.clear()
        backend.offline = false
        notes("sync")

        assertContains(notes("show n1").stdout, "This note was deleted elsewhere.")
        notes("resolve n1 mine")
        assertEquals(
            "Mine",
            backend.notes.values
                .single()
                .title,
        )
    }

    @Test
    fun aNoteWithoutConflictHasNothingToResolve() {
        signedIn()
        backend.add("Groceries")

        assertContains(notes("resolve n1 mine").stderr, "This note does not exist, or it is not yours.")
    }

    @Test
    fun aTitleOverTheLimitIsRejectedBeforeAnyCall() {
        signedIn()

        assertContains(notes("add --title ${"t".repeat(201)}").stderr, "title: must be at most 200 characters")
    }

    @Test
    fun anEndedSessionStopsTheSync() {
        signedIn()
        backend.offline = true
        notes("add --title Groceries")
        backend.offline = false
        backend.refreshAllowed = false
        runBlocking { tokens.save(Tokens("expired", "refresh", null)) }

        assertContains(notes("sync").stderr, "Not signed in: run notes login.")
    }

    @Test
    fun aNoteNeverReadCannotBeShownOffline() {
        signedIn()
        backend.offline = true

        assertContains(notes("show n1").stderr, "The service cannot be reached. Try again.")
    }
}
