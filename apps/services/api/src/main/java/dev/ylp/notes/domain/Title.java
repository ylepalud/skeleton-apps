package dev.ylp.notes.domain;

import dev.ylp.shared.error.business.BusinessException;
import dev.ylp.shared.error.business.InvalidRecordReason;
import org.jmolecules.ddd.annotation.ValueObject;

/** The title of a note. */
@ValueObject
public record Title(String value) {

  /** Longest title accepted. */
  public static final int MAX_LENGTH = 200;

  /** Rejects a blank or oversized title. */
  public Title {
    if (value.isBlank() || value.length() > MAX_LENGTH) {
      throw new BusinessException(
          InvalidRecordReason.CONSTRAINT_VIOLATION,
          "A title must be non-blank and at most " + MAX_LENGTH + " characters long");
    }
  }
}
