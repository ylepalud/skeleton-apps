// swift-format-ignore-file: AvoidRetroactiveConformances

import Shared

// Swift export flattens the root package of the shared module only: these names spare the app
// the full Kotlin package of each type it uses.
typealias AuthConfig = ExportedKotlinPackages.dev.ylp.client.auth.domain.AuthConfig
typealias AuthorizationBrowser = ExportedKotlinPackages.dev.ylp.client.auth.domain.AuthorizationBrowser
typealias TokenStore = ExportedKotlinPackages.dev.ylp.client.auth.domain.TokenStore
typealias Tokens = ExportedKotlinPackages.dev.ylp.client.auth.domain.Tokens
typealias Session = ExportedKotlinPackages.dev.ylp.client.auth.application.Session
typealias ListNotes = ExportedKotlinPackages.dev.ylp.client.notes.application.ListNotes
typealias Note = ExportedKotlinPackages.dev.ylp.client.notes.domain.Note
typealias NoteDraft = ExportedKotlinPackages.dev.ylp.client.notes.domain.NoteDraft
typealias NoteFailure = ExportedKotlinPackages.dev.ylp.client.notes.domain.NoteFailure
typealias NoteResult = ExportedKotlinPackages.dev.ylp.client.notes.domain.NoteResult
typealias Instant = ExportedKotlinPackages.kotlin.time.Instant

// The use cases hold only immutable references, and Kotlin/Native objects may be used from any
// thread: the screens call them from the main actor, and the shared module runs them on its own.
extension ExportedKotlinPackages.dev.ylp.client.auth.application.CurrentSession: @retroactive @unchecked Sendable {}
extension ExportedKotlinPackages.dev.ylp.client.auth.application.SignIn: @retroactive @unchecked Sendable {}
extension ExportedKotlinPackages.dev.ylp.client.auth.application.SignOut: @retroactive @unchecked Sendable {}
extension ListNotes: @retroactive @unchecked Sendable {}
extension ExportedKotlinPackages.dev.ylp.client.notes.application.GetNote: @retroactive @unchecked Sendable {}
extension ExportedKotlinPackages.dev.ylp.client.notes.application.CreateNote: @retroactive @unchecked Sendable {}
extension ExportedKotlinPackages.dev.ylp.client.notes.application.UpdateNote: @retroactive @unchecked Sendable {}
extension ExportedKotlinPackages.dev.ylp.client.notes.application.DeleteNote: @retroactive @unchecked Sendable {}
