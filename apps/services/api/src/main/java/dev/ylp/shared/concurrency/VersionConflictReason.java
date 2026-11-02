package dev.ylp.shared.concurrency;

import dev.ylp.shared.error.business.BusinessErrorReason;
import dev.ylp.shared.error.business.BusinessException;
import dev.ylp.shared.error.business.ErrorCategory;

/** Why a change to a versioned resource was refused, whatever the module. */
public enum VersionConflictReason implements BusinessErrorReason {
  VERSION_MISMATCH(
      "Read the resource again: it changed since your copy. Then apply your change to it.",
      ErrorCategory.STALE_VERSION),
  VERSION_REQUIRED(
      "Send the ETag you last read in an If-Match header.", ErrorCategory.VERSION_REQUIRED);

  private final String hint;
  private final ErrorCategory category;

  VersionConflictReason(String hint, ErrorCategory category) {
    this.hint = hint;
    this.category = category;
  }

  @Override
  public String hint() {
    return hint;
  }

  @Override
  public ErrorCategory category() {
    return category;
  }

  /** Returns the error for a change based on a version that is no longer the current one. */
  public static BusinessException mismatch(String detail) {
    return new BusinessException(VERSION_MISMATCH, detail);
  }

  /** Returns the error for a change that names no version. */
  public static BusinessException required(String detail) {
    return new BusinessException(VERSION_REQUIRED, detail);
  }
}
