package dev.ylp.notes.domain;

import dev.ylp.shared.error.business.BusinessException;
import dev.ylp.shared.error.business.InvalidRecordReason;

/** A page of results to read, numbered from zero. */
public record PageRequest(int page, int size) {

  /** Largest page a caller may request. */
  public static final int MAX_SIZE = 100;

  /** Rejects a negative page number or a size outside 1 to {@value #MAX_SIZE}. */
  public PageRequest {
    if (page < 0 || size < 1 || size > MAX_SIZE) {
      throw new BusinessException(
          InvalidRecordReason.CONSTRAINT_VIOLATION,
          "A page must be zero or more and its size between 1 and " + MAX_SIZE);
    }
  }

  /** Returns the number of items before this page. */
  public long offset() {
    return (long) page * size;
  }
}
