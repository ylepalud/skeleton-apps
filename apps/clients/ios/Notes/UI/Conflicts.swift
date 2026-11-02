import Shared

extension NoteConflict {
    /// The resolutions that settle this conflict, in the order the editor shows them.
    var resolutions: [Resolution] { [.KEEP_MINE, .KEEP_THEIRS, .KEEP_BOTH].filter(allows(resolution:)) }

    /// What the user reads about a conflict: what happened to the note.
    var headline: String {
        switch kind {
        case .CHANGED_ELSEWHERE:
            mineDeleted ? "You deleted this note, and it was changed elsewhere." : "This note was changed elsewhere."
        case .DELETED_ELSEWHERE: "This note was deleted elsewhere."
        case .REJECTED: "The service refused this change."
        }
    }

    /// The button of a resolution, in the words of this conflict.
    func label(_ resolution: Resolution) -> String {
        switch resolution {
        case .KEEP_MINE:
            if mineDeleted {
                "Delete anyway"
            } else if kind == .DELETED_ELSEWHERE {
                "Restore mine"
            } else {
                "Keep mine"
            }
        case .KEEP_THEIRS: theirs == nil ? "Discard mine" : "Keep theirs"
        case .KEEP_BOTH: "Keep both"
        }
    }
}

extension SyncState {
    /// The label of a note in the list, when the API does not have it as shown.
    var label: String? {
        switch self {
        case .SYNCED: nil
        case .PENDING: "Not synced"
        case .CONFLICTING: "Conflict: open to resolve"
        }
    }

    /// What the list says when the API cannot be reached.
    static let offline = "Offline: these are the notes kept on this device."
}
