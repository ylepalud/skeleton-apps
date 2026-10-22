package dev.ylp.notes.domain;

import dev.ylp.shared.error.business.BusinessException;
import dev.ylp.shared.error.business.InvalidRecordReason;

/** The user owning a note: the subject of the identity provider's token. */
public record OwnerId(String value) {

  /** Longest subject accepted, matching the persisted column. */
  public static final int MAX_LENGTH = 255;

  /** Rejects a blank or oversized subject. */
  public OwnerId {
    if (value.isBlank() || value.length() > MAX_LENGTH) {
      throw new BusinessException(
          InvalidRecordReason.CONSTRAINT_VIOLATION,
          "An owner id must be non-blank and at most " + MAX_LENGTH + " characters long");
    }
  }
}
