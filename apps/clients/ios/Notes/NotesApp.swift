import Shared
import SwiftUI

/// The app: SwiftUI screens and the two platform adapters of the shared module.
@main
struct NotesApp: App {
    private let client = Composition.client(AppConfiguration.current())

    var body: some Scene {
        WindowGroup {
            RootView(client: client)
        }
    }
}
