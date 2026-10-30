import Shared
import SwiftUI

/// Sign-in, or the notes once signed in; an ended session leads back to sign-in.
struct RootView: View {
    let client: SharedClient
    /// Nil while the stored session is read.
    @State private var signedIn: Bool?

    var body: some View {
        Group {
            switch signedIn {
            case nil: ProgressView()
            case false?: SignInView(client: client) { signedIn = true }
            case true?: NotesNavigation(client: client) { signedIn = false }
            }
        }
        .task {
            let session = try? await client.currentSession.execute()
            signedIn = session is Session.SignedIn
        }
    }
}

/// The list of notes, and the editor pushed over it.
struct NotesNavigation: View {
    let client: SharedClient
    let onSignedOut: () -> Void
    @State private var path: [Route] = []

    enum Route: Hashable {
        case new
        case edit(String)
    }

    var body: some View {
        NavigationStack(path: $path) {
            NotesView(
                client: client,
                onOpen: { path.append(.edit($0)) },
                onNew: { path.append(.new) },
                onSignedOut: onSignedOut
            )
            .navigationDestination(for: Route.self) { route in
                NoteEditorView(
                    client: client,
                    id: route.id,
                    onDone: { path.removeLast() },
                    onSignedOut: onSignedOut
                )
            }
        }
    }
}

extension NotesNavigation.Route {
    fileprivate var id: String? {
        if case .edit(let id) = self { id } else { nil }
    }
}
