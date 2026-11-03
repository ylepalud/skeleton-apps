// swift-format-ignore-file: AvoidRetroactiveConformances

import Shared

// Swift export flattens the root package of the shared module only: these names spare the app
// the full Kotlin package of each type it uses. `Client` keeps every line short, whatever the
// project's package.
typealias Client = ExportedKotlinPackages.dev.ylp.client
typealias AuthConfig = Client.auth.domain.AuthConfig
typealias AuthorizationBrowser = Client.auth.domain.AuthorizationBrowser
typealias TokenStore = Client.auth.domain.TokenStore
typealias Tokens = Client.auth.domain.Tokens
typealias Session = Client.auth.application.Session
typealias ListNotes = Client.notes.application.ListNotes
typealias Note = Client.notes.domain.Note
typealias NoteDraft = Client.notes.domain.NoteDraft
typealias NoteFailure = Client.notes.domain.NoteFailure
typealias NoteResult = Client.notes.domain.NoteResult
typealias SyncState = Client.notes.domain.SyncState
typealias NoteConflict = Client.notes.domain.NoteConflict
typealias NoteConflictResult = Client.notes.domain.NoteConflictResult
typealias ConflictKind = Client.notes.domain.ConflictKind
typealias Resolution = Client.notes.domain.Resolution
typealias ResolutionResult = Client.notes.domain.ResolutionResult
typealias Instant = ExportedKotlinPackages.kotlin.time.Instant

// The use cases hold only immutable references, and Kotlin/Native objects may be used from any
// thread: the screens call them from the main actor, and the shared module runs them on its own.
extension Client.auth.application.CurrentSession: @retroactive @unchecked Sendable {}
extension Client.auth.application.SignIn: @retroactive @unchecked Sendable {}
extension Client.auth.application.SignOut: @retroactive @unchecked Sendable {}
extension ListNotes: @retroactive @unchecked Sendable {}
extension Client.notes.application.GetNote: @retroactive @unchecked Sendable {}
extension Client.notes.application.CreateNote: @retroactive @unchecked Sendable {}
extension Client.notes.application.UpdateNote: @retroactive @unchecked Sendable {}
extension Client.notes.application.DeleteNote: @retroactive @unchecked Sendable {}
extension Client.notes.application.GetConflict: @retroactive @unchecked Sendable {}
extension Client.notes.application.ResolveConflict: @retroactive @unchecked Sendable {}
extension Client.sync.application.SyncChanges: @retroactive @unchecked Sendable {}
extension Client.sync.application.CountPendingChanges: @retroactive @unchecked Sendable {}

// An enum constant of Kotlin never changes.
extension Resolution: @retroactive @unchecked Sendable {}
