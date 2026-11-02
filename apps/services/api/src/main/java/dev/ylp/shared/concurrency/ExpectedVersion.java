package dev.ylp.shared.concurrency;

import org.jspecify.annotations.Nullable;

/**
 * The version of a resource a change is based on, as the caller last read it: optimistic
 * concurrency. {@link #ANY} accepts whatever the current version is.
 *
 * @param value the expected version, or null for any
 */
public record ExpectedVersion(@Nullable Long value) {

  /** Accepts any current version: the caller chose to overwrite. */
  public static final ExpectedVersion ANY = new ExpectedVersion(null);

  /** Expects exactly this version. */
  public static ExpectedVersion of(long value) {
    return new ExpectedVersion(value);
  }

  /** Returns whether a resource at this version may be changed. */
  public boolean matches(long current) {
    return value == null || value == current;
  }

  /**
   * Raises {@link VersionConflictReason#VERSION_MISMATCH} unless a resource at this version may be
   * changed.
   *
   * @param what names the resource in the error, such as "Note 3f0c..."
   */
  public void check(long current, String what) {
    if (!matches(current)) {
      throw VersionConflictReason.mismatch(what + " is at version " + current + ", not " + value);
    }
  }
}
