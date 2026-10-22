package dev.ylp.notes.domain;

import dev.ylp.shared.error.business.BusinessException;
import dev.ylp.shared.error.business.InvalidRecordReason;

/** The body of a note, possibly empty. */
public record Content(String value) {

  /** Longest content accepted. */
  public static final int MAX_LENGTH = 20_000;

  /** Rejects an oversized content. */
  public Content {
    if (value.length() > MAX_LENGTH) {
      throw new BusinessException(
          InvalidRecordReason.CONSTRAINT_VIOLATION,
          "A content must be at most " + MAX_LENGTH + " characters long");
    }
  }
}
