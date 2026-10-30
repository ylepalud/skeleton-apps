#if DEBUG
import Foundation
import Shared

/// UI tests only: answers what the provider's page would, without showing it. Not in release builds.
nonisolated final class ScriptedBrowser: PlatformAdapter, AuthorizationBrowser, @unchecked Sendable {
    private let cancels: Bool

    init(cancels: Bool) {
        self.cancels = cancels
        super.init()
    }

    func authorize(url: String, redirectUri: String) async throws -> String? {
        if cancels { return nil }
        let state = URLComponents(string: url)?.queryItems?.first { $0.name == "state" }?.value ?? ""
        return "\(redirectUri)?code=test-code&state=\(state)"
    }
}
#endif
