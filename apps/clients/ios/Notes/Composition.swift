import Network
import Shared

/// Builds the shared client once, with the adapters this platform provides.
enum Composition {
    static func client(_ configuration: AppConfiguration) -> SharedClient {
        let tokenStore = KeychainTokenStore()
        if configuration.resetsSession {
            tokenStore.remove()
            deleteLocalStore(name: DEFAULT_STORE_NAME)
        }
        return SharedClient(
            apiBaseUrl: configuration.apiBaseUrl,
            auth: AuthConfig(
                issuer: configuration.issuer,
                clientId: configuration.clientId,
                redirectUri: configuration.redirectUri,
                scopes: ["openid", "offline_access"]
            ),
            browser: browser(configuration),
            localStore: openLocalStore(name: DEFAULT_STORE_NAME),
            tokenStore: tokenStore
        )
    }

    private static func browser(_ configuration: AppConfiguration) -> any AuthorizationBrowser {
        #if DEBUG
        if let testSignIn = configuration.testSignIn {
            return ScriptedBrowser(cancels: testSignIn == .cancel)
        }
        #endif
        return WebAuthenticationBrowser()
    }
}

/// Sends the changes made offline as soon as a network is back.
final class NetworkSync {
    private let monitor = NWPathMonitor()

    init(client: SharedClient) {
        let syncChanges = client.syncChanges
        monitor.pathUpdateHandler = { path in
            if path.status == .satisfied {
                Task { _ = try? await syncChanges.execute() }
            }
        }
        monitor.start(queue: DispatchQueue(label: "dev.ylp.notes.network"))
    }

    deinit { monitor.cancel() }
}
