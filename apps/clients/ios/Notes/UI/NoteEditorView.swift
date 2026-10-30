import Shared
import SwiftUI

/// Writes a new note when `id` is nil, otherwise edits or deletes that note.
@Observable
final class NoteEditorModel {
    var title = ""
    var content = ""
    var confirmingDelete = false
    private(set) var problem: String?
    private(set) var done = false
    private(set) var signedOut = false
    let editing: Bool
    private let id: String?
    private let client: SharedClient

    init(client: SharedClient, id: String?) {
        self.client = client
        self.id = id
        editing = id != nil
    }

    func load() async {
        guard let id, let note = await note(from: { try await self.client.getNote.execute(id: id) }) else { return }
        title = note.title
        content = note.content
    }

    func save() async {
        let (id, title, content) = (id, title, content)
        let saved = await note {
            if let id {
                try await self.client.updateNote.execute(id: id, title: title, content: content)
            } else {
                try await self.client.createNote.execute(title: title, content: content)
            }
        }
        if saved != nil { done = true }
    }

    func delete() async {
        guard let id else { return }
        confirmingDelete = false
        do {
            switch try await client.deleteNote.execute(id: id).sealedType() {
            case .deleted: done = true
            case .failed(let failed): fail(failed.value.failure)
            }
        } catch {
            problem = NoteFailure.unreachable
        }
    }

    /// The note of a successful call; otherwise nil, with the failure reported.
    private func note(from call: () async throws -> NoteResult) async -> Note? {
        do {
            switch try await call().sealedType() {
            case .success(let success): return success.value.note
            case .failed(let failed): fail(failed.value.failure)
            }
        } catch {
            problem = NoteFailure.unreachable
        }
        return nil
    }

    private func fail(_ failure: NoteFailure) {
        if failure.endsSession { signedOut = true } else { problem = failure.message }
    }
}

struct NoteEditorView: View {
    @State private var model: NoteEditorModel
    private let onDone: () -> Void
    private let onSignedOut: () -> Void
    private static let titleMaxLength = Int(NoteDraft.Companion.shared.TITLE_MAX_LENGTH)
    private static let contentMaxLength = Int(NoteDraft.Companion.shared.CONTENT_MAX_LENGTH)

    init(client: SharedClient, id: String?, onDone: @escaping () -> Void, onSignedOut: @escaping () -> Void) {
        _model = State(initialValue: NoteEditorModel(client: client, id: id))
        self.onDone = onDone
        self.onSignedOut = onSignedOut
    }

    var body: some View {
        Form {
            TextField("Title", text: $model.title)
                .accessibilityLabel("Title")
                .onChange(of: model.title) { model.title = String(model.title.prefix(Self.titleMaxLength)) }
            TextField("Content", text: $model.content, axis: .vertical)
                // A filled field loses its placeholder: VoiceOver still names it.
                .accessibilityLabel("Content")
                .lineLimit(4...)
                .onChange(of: model.content) { model.content = String(model.content.prefix(Self.contentMaxLength)) }
            if let problem = model.problem {
                Text(problem).foregroundStyle(.red)
            }
            Section {
                Button(model.editing ? "Save" : "Add note") { Task { await model.save() } }
                if model.editing {
                    Button("Delete", role: .destructive) { model.confirmingDelete = true }
                }
            }
        }
        .navigationTitle(model.editing ? "Edit note" : "New note")
        .navigationBarTitleDisplayMode(.inline)
        .alert("Delete this note?", isPresented: $model.confirmingDelete) {
            Button("Delete", role: .destructive) { Task { await model.delete() } }
            Button("Keep", role: .cancel) {}
        }
        .task { await model.load() }
        .onChange(of: model.done) { if model.done { onDone() } }
        .onChange(of: model.signedOut) { if model.signedOut { onSignedOut() } }
    }
}
