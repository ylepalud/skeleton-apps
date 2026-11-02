import Shared
import SwiftUI

/// The app: SwiftUI screens and the two platform adapters of the shared module.
@main
struct NotesApp: App {
    private let client: SharedClient
    private let networkSync: NetworkSync

    init() {
        client = Composition.client(AppConfiguration.current())
        networkSync = NetworkSync(client: client)
    }

    var body: some Scene {
        WindowGroup {
            RootView(client: client)
        }
    }
}
