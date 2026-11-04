import XCTest

/// The notes flow on the real screens and the real shared client, against a stub backend.
@MainActor
final class NotesFlowTests: XCTestCase {
    // Set by setUp and stopped by tearDown, which XCTest runs outside the main actor, never
    // while a test runs; the stub itself is thread-safe.
    nonisolated(unsafe) private var backend: StubBackend!
    private let app = XCUIApplication()
    private static let timeout: TimeInterval = 10
    private static let offline = "Offline: these are the notes kept on this device."

    override func setUpWithError() throws {
        continueAfterFailure = false
        backend = try StubBackend()
        try backend.start()
    }

    override func tearDown() {
        backend.stop()
    }

    func testSigningInShowsTheUsersNotes() {
        backend.add("Groceries", content: "Milk, eggs")

        signIn()

        waitFor("Groceries")
        waitFor("Your notes (1)")
    }

    func testClosingTheSignInPageCancelsTheSignIn() {
        launch(signIn: "cancel")

        app.buttons["Sign in"].tap()

        waitFor("Sign-in cancelled.")
    }

    func testANewNoteIsWrittenAndListed() {
        signIn()
        waitFor("No notes yet.")

        app.buttons["New note"].tap()
        type("Groceries", into: "Title")
        type("Milk", into: "Content")
        app.buttons["Add note"].tap()

        waitFor("Your notes (1)")
        waitFor("Groceries")
        XCTAssertEqual(backend.notes, [.init(title: "Groceries", content: "Milk")])
    }

    func testAnEmptyTitleIsRejectedWithoutCallingTheApi() {
        signIn()
        waitFor("No notes yet.")

        app.buttons["New note"].tap()
        app.buttons["Add note"].tap()

        waitFor("title: must not be empty")
        XCTAssertFalse(backend.requests.contains("POST /api/v1/notes"))
    }

    func testANoteIsEdited() {
        backend.add("Groceries", content: "Milk")
        signIn()

        waitFor("Groceries").tap()
        waitFor("Edit note")
        let title = app.textFields["Title"]
        expectation(for: NSPredicate(format: "value == %@", "Groceries"), evaluatedWith: title)
        waitForExpectations(timeout: Self.timeout)
        // At the end of the text, so that the deletes clear it.
        title.coordinate(withNormalizedOffset: CGVector(dx: 0.95, dy: 0.5)).tap()
        title.typeText(
            String(repeating: XCUIKeyboardKey.delete.rawValue, count: "Groceries".count) + "Weekend groceries")
        app.buttons["Save"].tap()

        waitFor("Weekend groceries")
        XCTAssertEqual(backend.notes, [.init(title: "Weekend groceries", content: "Milk")])
    }

    func testANoteChangedElsewhereIsKeptAsMine() {
        changeOnBothSides()

        app.buttons["Keep mine"].tap()

        waitFor("Your notes (1)")
        XCTAssertEqual(backend.notes, [.init(title: "From the phone", content: "Milk")])
    }

    func testANoteChangedElsewhereIsKeptAsTheirs() {
        changeOnBothSides()

        app.buttons["Keep theirs"].tap()

        waitFor("Your notes (1)")
        XCTAssertEqual(backend.notes, [.init(title: "From the browser", content: "Eggs")])
    }

    func testANoteChangedElsewhereIsKeptOnBothSides() {
        changeOnBothSides()

        app.buttons["Keep both"].tap()

        waitFor("Your notes (2)")
        XCTAssertEqual(Set(backend.notes.map(\.title)), ["From the phone", "From the browser"])
    }

    func testANoteWrittenOfflineIsListedThenSentWhenTheApiIsBack() {
        backend.add("Groceries")
        signIn()
        waitFor("Groceries")
        backend.offline = true

        app.buttons["New note"].tap()
        type("Books", into: "Title")
        app.buttons["Add note"].tap()

        waitFor(Self.offline)
        waitFor("Not synced")
        backend.offline = false
        app.buttons["Retry"].tap()

        waitFor("Your notes (2)")
        XCTAssertEqual(Set(backend.notes.map(\.title)), ["Groceries", "Books"])
    }

    func testSigningOutWithChangesNotSyncedAsksFirst() {
        signIn()
        waitFor("No notes yet.")
        backend.offline = true
        app.buttons["New note"].tap()
        type("Books", into: "Title")
        app.buttons["Add note"].tap()
        waitFor("Not synced")

        app.buttons["Sign out"].tap()
        XCTAssertTrue(app.alerts.buttons["Stay"].waitForExistence(timeout: Self.timeout))
        app.alerts.buttons["Stay"].tap()
        app.buttons["Sign out"].tap()
        XCTAssertTrue(app.alerts.buttons["Sign out anyway"].waitForExistence(timeout: Self.timeout))
        app.alerts.buttons["Sign out anyway"].tap()

        XCTAssertTrue(app.buttons["Sign in"].waitForExistence(timeout: Self.timeout))
    }

    func testANoteIsDeletedOnlyOnceConfirmed() {
        backend.add("Groceries")
        signIn()
        waitFor("Groceries").tap()
        waitFor("Edit note")

        app.buttons["Delete"].tap()
        app.alerts.buttons["Keep"].tap()
        XCTAssertEqual(backend.notes.count, 1)
        app.buttons["Delete"].tap()
        XCTAssertTrue(app.alerts["Delete this note?"].waitForExistence(timeout: Self.timeout))
        app.alerts.buttons["Delete"].tap()

        waitFor("No notes yet.")
        XCTAssertEqual(backend.notes.count, 0)
    }

    func testANoteGoneMeanwhileIsReportedAsNotFound() {
        let id = backend.add("Groceries")
        signIn()
        waitFor("Groceries")
        backend.remove(id)

        app.buttons["Groceries"].tap()

        waitFor("This note does not exist, or it is not yours.")
    }

    func testMoreNotesLoadOnRequest() {
        for index in 0..<25 { backend.add("Note \(index)") }
        signIn()
        waitFor("Your notes (25)")

        let loadMore = app.buttons["Load more"]
        while !loadMore.isHittable { app.swipeUp() }
        loadMore.tap()

        let last = app.buttons["Note 24"]
        for _ in 0..<5 where !last.exists { app.swipeUp() }
        XCTAssertTrue(last.waitForExistence(timeout: Self.timeout))
    }

    func testAnUnreachableApiCanBeRetried() {
        backend.add("Groceries")
        backend.failNext(with: 503)
        signIn()

        waitFor(Self.offline)
        app.buttons["Retry"].tap()

        waitFor("Groceries")
    }

    func testAnEndedSessionLeadsBackToSignIn() {
        signIn()
        waitFor("No notes yet.")
        backend.endSessions()

        app.buttons["New note"].tap()
        type("Groceries", into: "Title")
        app.buttons["Add note"].tap()

        XCTAssertTrue(app.buttons["Sign in"].waitForExistence(timeout: Self.timeout))
        XCTAssertEqual(backend.notes.count, 0)
    }

    func testSigningOutLeadsBackToSignIn() {
        signIn()
        waitFor("No notes yet.")

        app.buttons["Sign out"].tap()
        XCTAssertTrue(app.buttons["Sign in"].waitForExistence(timeout: Self.timeout))

        // The session is gone for good: a new launch asks to sign in again.
        launch(resetsSession: false)
        XCTAssertTrue(app.buttons["Sign in"].waitForExistence(timeout: Self.timeout))
    }

    func testTheSessionIsKeptBetweenLaunches() {
        backend.add("Groceries")
        signIn()
        waitFor("Groceries")

        launch(resetsSession: false)

        waitFor("Groceries")
    }

    // MARK: Helpers

    /// A note opened here, changed elsewhere, then saved here: the editor shows the conflict.
    private func changeOnBothSides() {
        let id = backend.add("Groceries", content: "Milk")
        signIn()
        waitFor("Groceries").tap()
        waitFor("Edit note")
        let title = app.textFields["Title"]
        expectation(for: NSPredicate(format: "value == %@", "Groceries"), evaluatedWith: title)
        waitForExpectations(timeout: Self.timeout)
        backend.changeElsewhere(id, title: "From the browser", content: "Eggs")

        title.coordinate(withNormalizedOffset: CGVector(dx: 0.95, dy: 0.5)).tap()
        title.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: "Groceries".count) + "From the phone")
        app.buttons["Save"].tap()

        waitFor("This note was changed elsewhere.")
        waitFor("From the browser")
        XCTAssertEqual(backend.notes, [.init(title: "From the browser", content: "Eggs")])
    }

    private func launch(signIn: String = "redirect", resetsSession: Bool = true) {
        app.terminate()
        app.launchEnvironment = [
            "NOTES_API_BASE_URL": backend.url("/api/v1"),
            "NOTES_OIDC_ISSUER": backend.url("/idp"),
            "NOTES_TEST_SIGN_IN": signIn,
            "NOTES_RESET_SESSION": resetsSession ? "1" : "0",
        ]
        app.launch()
    }

    private func signIn() {
        launch()
        let button = app.buttons["Sign in"]
        XCTAssertTrue(button.waitForExistence(timeout: Self.timeout))
        button.tap()
    }

    @discardableResult
    private func waitFor(_ text: String) -> XCUIElement {
        let element = app.descendants(matching: .any).matching(NSPredicate(format: "label == %@", text)).firstMatch
        XCTAssertTrue(element.waitForExistence(timeout: Self.timeout), "\(text) never showed")
        return element
    }

    private func type(_ text: String, into field: String) {
        let element = app.textFields[field]
        element.tap()
        element.typeText(text)
    }
}
