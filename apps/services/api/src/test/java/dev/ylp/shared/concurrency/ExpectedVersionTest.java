package dev.ylp.shared.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.ylp.shared.error.business.BusinessException;
import dev.ylp.shared.error.business.ErrorCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExpectedVersionTest {

  @Test
  @DisplayName("An expected version matches that version only")
  void expectedVersionMatchesThatVersionOnly() {
    // Given
    ExpectedVersion expected = ExpectedVersion.of(2);

    // When / Then
    assertThat(expected.matches(2)).isTrue();
    assertThat(expected.matches(3)).isFalse();
    assertThat(ExpectedVersion.ANY.matches(3)).isTrue();
  }

  @Test
  @DisplayName("Checking a stale version fails with the resource and both versions")
  void checkingStaleVersionFailsWithTheResourceAndBothVersions() {
    // When / Then
    assertThatCode(() -> ExpectedVersion.of(3).check(3, "Note x")).doesNotThrowAnyException();
    assertThatThrownBy(() -> ExpectedVersion.of(2).check(3, "Note x"))
        .isInstanceOfSatisfying(
            BusinessException.class,
            e -> {
              assertThat(e.reason()).isEqualTo(VersionConflictReason.VERSION_MISMATCH);
              assertThat(e.reason().category()).isEqualTo(ErrorCategory.STALE_VERSION);
              assertThat(e).hasMessage("Note x is at version 3, not 2");
            });
  }

  @Test
  @DisplayName("A change without a version is its own error")
  void changeWithoutVersionIsItsOwnError() {
    // When
    BusinessException error = VersionConflictReason.required("No If-Match");

    // Then
    assertThat(error.reason().category()).isEqualTo(ErrorCategory.VERSION_REQUIRED);
    assertThat(error.hint()).contains("If-Match");
  }
}
