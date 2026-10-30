import Shared
import SwiftUI

@Observable
final class NotesModel {
    private(set) var notes: [Note] = []
    private(set) var totalNotes: Int64 = 0
    private(set) var hasNext = false
    private(set) var loading = false
    private(set) var problem: String?
    private(set) var signedOut = false
    private var page: Int32 = 0
    private let client: SharedClient

    init(client: SharedClient) {
        self.client = client
    }

    /// Reloads from the first page, as when coming back from the editor.
    func refresh() async { await load(0) }

    func loadMore() async { await load(page + 1) }

    func signOut() async {
        try? await client.signOut.execute()
        signedOut = true
    }

    private func load(_ page: Int32) async {
        loading = true
        defer { loading = false }
        do {
            let result = try await client.listNotes.execute(
                page: page, size: ListNotes.Companion.shared.DEFAULT_PAGE_SIZE)
            switch result.sealedType() {
            case .success(let success):
                let loaded = success.value.page
                self.page = page
                notes = page == 0 ? loaded.notes : notes + loaded.notes
                totalNotes = loaded.totalNotes
                hasNext = loaded.hasNext
                problem = nil
            case .failed(let failed):
                if failed.value.failure.endsSession { signedOut = true } else { problem = failed.value.failure.message }
            }
        } catch {
            problem = NoteFailure.unreachable
        }
    }
}

struct NotesView: View {
    @State private var model: NotesModel
    private let onOpen: (String) -> Void
    private let onNew: () -> Void
    private let onSignedOut: () -> Void

    init(
        client: SharedClient,
        onOpen: @escaping (String) -> Void,
        onNew: @escaping () -> Void,
        onSignedOut: @escaping () -> Void
    ) {
        _model = State(initialValue: NotesModel(client: client))
        self.onOpen = onOpen
        self.onNew = onNew
        self.onSignedOut = onSignedOut
    }

    var body: some View {
        List {
            if let problem = model.problem {
                Section {
                    Text(problem).foregroundStyle(.red)
                    Button("Retry") { Task { await model.refresh() } }
                }
            }
            if !model.loading && model.problem == nil && model.notes.isEmpty {
                Text("No notes yet.")
            }
            ForEach(model.notes, id: \.id) { note in
                Button {
                    onOpen(note.id)
                } label: {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(note.title).font(.headline)
                        if !note.content.isEmpty {
                            Text(note.content).lineLimit(3).foregroundStyle(.secondary)
                        }
                    }
                }
                .foregroundStyle(.primary)
            }
            if model.hasNext {
                Button("Load more") { Task { await model.loadMore() } }
                    .disabled(model.loading)
            }
        }
        .navigationTitle("Your notes (\(model.totalNotes))")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button("Sign out") { Task { await model.signOut() } }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button("New note", action: onNew)
            }
        }
        .onAppear { Task { await model.refresh() } }
        .onChange(of: model.signedOut) { if model.signedOut { onSignedOut() } }
    }
}
