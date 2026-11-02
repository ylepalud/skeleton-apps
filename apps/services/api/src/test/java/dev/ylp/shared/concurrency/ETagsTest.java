package dev.ylp.shared.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.ylp.shared.error.business.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ETagsTest {

  @Test
  @DisplayName("A version's ETag is the version, quoted, and If-Match reads it back")
  void versionsETagIsTheVersionQuotedAndIfMatchReadsItBack() {
    // When
    String etag = ETags.of(42);

    // Then
    assertThat(etag).isEqualTo("\"42\"");
    assertThat(ETags.expected(etag)).isEqualTo(ExpectedVersion.of(42));
    assertThat(ETags.expected(" " + etag + " ")).isEqualTo(ExpectedVersion.of(42));
  }

  @Test
  @DisplayName("If-Match * expects any version")
  void ifMatchStarExpectsAnyVersion() {
    // When / Then
    assertThat(ETags.expected("*")).isEqualTo(ExpectedVersion.ANY);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"W/\"1\"", "1", "\"one\"", "\"\"", "\"1\", \"2\"", "\"1234567890123456789\""})
  @DisplayName("An If-Match that names no version of this API can never match")
  void ifMatchThatNamesNoVersionCanNeverMatch(String ifMatch) {
    // When / Then
    assertThatThrownBy(() -> ETags.expected(ifMatch))
        .isInstanceOfSatisfying(
            BusinessException.class,
            e -> assertThat(e.reason()).isEqualTo(VersionConflictReason.VERSION_MISMATCH));
  }
}
