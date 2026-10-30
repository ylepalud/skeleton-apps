import Shared

extension NoteFailure {
    /// What a user reads for a failure. `SignedOut` never shows: it leads to sign-in.
    var message: String {
        switch sealedType() {
        case .notFound: "This note does not exist, or it is not yours."
        case .invalid(let invalid):
            invalid.value.violations.map { [$0.field, $0.message].compactMap(\.self).joined(separator: ": ") }
                .joined(separator: "\n")
        case .signedOut: "Your session has ended."
        case .unavailable: Self.unreachable
        }
    }

    var endsSession: Bool { self is NoteFailure.SignedOut }

    /// Also shown when the call itself fails, before any answer.
    static let unreachable = "The service cannot be reached. Try again."
}
