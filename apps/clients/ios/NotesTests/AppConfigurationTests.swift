import XCTest

@testable import Notes

@MainActor
final class AppConfigurationTests: XCTestCase {
    private let info = [
        "NotesApiBaseUrl": "https://api.example/api/v1", "NotesOidcIssuer": "https://idp.example",
        "NotesOidcClientId": "app",
    ]

    func testTheBuildSettingsAreRead() {
        let configuration = AppConfiguration.current(info: info, environment: [:])

        XCTAssertEqual(configuration.apiBaseUrl, "https://api.example/api/v1")
        XCTAssertEqual(configuration.issuer, "https://idp.example")
        XCTAssertEqual(configuration.clientId, "app")
        XCTAssertEqual(configuration.redirectUri, "dev.ylp.notes://callback")
        XCTAssertNil(configuration.testSignIn)
        XCTAssertFalse(configuration.resetsSession)
    }

    func testTheUiTestsPointTheAppAtTheirBackend() {
        let configuration = AppConfiguration.current(
            info: info,
            environment: [
                "NOTES_API_BASE_URL": "http://127.0.0.1:1/api/v1", "NOTES_OIDC_ISSUER": "http://127.0.0.1:1/idp",
                "NOTES_TEST_SIGN_IN": "cancel", "NOTES_RESET_SESSION": "1",
            ])

        XCTAssertEqual(configuration.apiBaseUrl, "http://127.0.0.1:1/api/v1")
        XCTAssertEqual(configuration.issuer, "http://127.0.0.1:1/idp")
        XCTAssertEqual(configuration.testSignIn, .cancel)
        XCTAssertTrue(configuration.resetsSession)
    }
}
