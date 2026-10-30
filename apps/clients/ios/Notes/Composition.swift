import Shared

/// Builds the shared client once, with the adapters this platform provides.
enum Composition {
    static func client(_ configuration: AppConfiguration) -> SharedClient {
        let tokenStore = KeychainTokenStore()
        if configuration.resetsSession {
            tokenStore.remove()
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
