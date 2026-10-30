import AuthenticationServices
import Shared
import UIKit

/// Shows the provider's sign-in page in ASWebAuthenticationSession, which shares the cookies of
/// Safari and hands the redirect back to the app. Closing the page cancels the sign-in.
nonisolated final class WebAuthenticationBrowser: PlatformAdapter, AuthorizationBrowser, @unchecked Sendable {
    /// Opens the page at the URL and calls back with the redirect to the scheme, or nil when cancelled.
    typealias Session = @MainActor (URL, String, @escaping @MainActor (URL?) -> Void) -> Void

    private let session: Session

    init(session: @escaping Session = WebAuthenticationBrowser.system) {
        self.session = session
        super.init()
    }

    func authorize(url: String, redirectUri: String) async throws -> String? {
        guard let page = URL(string: url), let scheme = URL(string: redirectUri)?.scheme else { return nil }
        return await withCheckedContinuation { continuation in
            Task { @MainActor in
                session(page, scheme) { continuation.resume(returning: $0?.absoluteString) }
            }
        }
    }

    @MainActor
    static func system(page: URL, scheme: String, completion: @escaping @MainActor (URL?) -> Void) {
        let session = ASWebAuthenticationSession(url: page, callback: .customScheme(scheme)) { redirect, _ in
            Task { @MainActor in completion(redirect) }
        }
        session.presentationContextProvider = KeyWindow.shared
        session.start()
    }
}

/// The window the sign-in page shows over.
private final class KeyWindow: NSObject, ASWebAuthenticationPresentationContextProviding {
    static let shared = KeyWindow()

    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        return scenes.flatMap(\.windows).first(where: \.isKeyWindow) ?? ASPresentationAnchor()
    }
}
