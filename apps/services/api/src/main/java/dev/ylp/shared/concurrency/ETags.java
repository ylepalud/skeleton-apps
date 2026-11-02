package dev.ylp.shared.concurrency;

import java.util.regex.Pattern;

/**
 * Versions as HTTP sees them: a strong ETag carrying the version, quoted, and the If-Match header a
 * change sends back. Used by the REST adapters of every versioned resource.
 */
public final class ETags {

  private static final Pattern QUOTED_VERSION = Pattern.compile("\"(\\d{1,18})\"");

  private ETags() {}

  /** Returns the ETag of a resource at this version: {@code "3"} for version 3. */
  public static String of(long version) {
    return "\"" + version + "\"";
  }

  /**
   * Returns the version an If-Match header expects. {@code *} expects any version; anything that is
   * not one of our ETags, such as a weak one, can never match and raises {@link
   * VersionConflictReason#VERSION_MISMATCH}.
   */
  public static ExpectedVersion expected(String ifMatch) {
    String value = ifMatch.strip();
    if (value.equals("*")) {
      return ExpectedVersion.ANY;
    }
    var matcher = QUOTED_VERSION.matcher(value);
    if (!matcher.matches()) {
      throw VersionConflictReason.mismatch("If-Match " + value + " names no version of this API");
    }
    return ExpectedVersion.of(Long.parseLong(matcher.group(1)));
  }
}
