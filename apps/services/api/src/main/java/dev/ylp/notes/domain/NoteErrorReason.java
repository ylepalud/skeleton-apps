package dev.ylp.notes.domain;

import dev.ylp.shared.error.business.BusinessErrorReason;
import dev.ylp.shared.error.business.BusinessException;
import dev.ylp.shared.error.business.ErrorCategory;

/** Why an operation on a note was refused. */
public enum NoteErrorReason implements BusinessErrorReason {
  NOTE_NOT_FOUND("Check the note id: it does not exist, or it belongs to another user.");

  private final String hint;

  NoteErrorReason(String hint) {
    this.hint = hint;
  }

  @Override
  public String hint() {
    return hint;
  }

  @Override
  public ErrorCategory category() {
    return ErrorCategory.NOT_FOUND;
  }

  /**
   * Returns the error for a note that does not exist or belongs to someone else. Both cases are
   * reported identically so that a caller cannot learn which notes exist.
   */
  public static BusinessException noteNotFound(NoteId id) {
    return new BusinessException(NOTE_NOT_FOUND, "Note not found: " + id.value());
  }
}
