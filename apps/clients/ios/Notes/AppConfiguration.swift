import Foundation

/// Where the app finds the API and the identity provider, from the build settings in Info.plist.
struct AppConfiguration {
    let apiBaseUrl: String
    let issuer: String
    let clientId: String
    /// A private-use scheme (RFC 8252): the reverse of a domain the project owns.
    let redirectUri = "dev.ylp.notes://callback"
    /// Debug builds only: how the UI tests answer the sign-in page, and whether to forget the session.
    let testSignIn: TestSignIn?
    let resetsSession: Bool

    enum TestSignIn: String {
        case redirect, cancel
    }

    static func current(
        info: [String: Any] = Bundle.main.infoDictionary ?? [:],
        environment: [String: String] = ProcessInfo.processInfo.environment
    ) -> AppConfiguration {
        func setting(_ key: String) -> String { info[key] as? String ?? "" }
        #if DEBUG
        // The UI tests point the app at their stub backend.
        return AppConfiguration(
            apiBaseUrl: environment["NOTES_API_BASE_URL"] ?? setting("NotesApiBaseUrl"),
            issuer: environment["NOTES_OIDC_ISSUER"] ?? setting("NotesOidcIssuer"),
            clientId: setting("NotesOidcClientId"),
            testSignIn: environment["NOTES_TEST_SIGN_IN"].flatMap(TestSignIn.init(rawValue:)),
            resetsSession: environment["NOTES_RESET_SESSION"] == "1"
        )
        #else
        return AppConfiguration(
            apiBaseUrl: setting("NotesApiBaseUrl"),
            issuer: setting("NotesOidcIssuer"),
            clientId: setting("NotesOidcClientId"),
            testSignIn: nil,
            resetsSession: false
        )
        #endif
    }
}
