package dev.ylp.notes.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.ylp.shared.error.business.BusinessException;
import dev.ylp.shared.error.business.InvalidRecordReason;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ValueObjectsTest {

  @Test
  @DisplayName("Value objects with the same value are equal")
  void valueObjectsWithSameValueAreEqual() {
    // When / Then
    assertThat(new Title("Ideas")).isEqualTo(new Title("Ideas"));
    assertThat(new Content("Text")).isEqualTo(new Content("Text"));
    assertThat(new OwnerId("ada")).isEqualTo(new OwnerId("ada"));
  }

  @Test
  @DisplayName("A title accepts up to its maximum length")
  void titleAcceptsUpToItsMaximumLength() {
    // When / Then
    assertThat(new Title("x".repeat(Title.MAX_LENGTH)).value()).hasSize(Title.MAX_LENGTH);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  @DisplayName("A blank title is rejected")
  void blankTitleIsRejected(String value) {
    // When / Then
    assertRejected(() -> new Title(value));
  }

  @Test
  @DisplayName("A title longer than its maximum is rejected")
  void titleLongerThanItsMaximumIsRejected() {
    // When / Then
    assertRejected(() -> new Title("x".repeat(Title.MAX_LENGTH + 1)));
  }

  @Test
  @DisplayName("A content may be empty or reach its maximum length")
  void contentMayBeEmptyOrReachItsMaximumLength() {
    // When / Then
    assertThat(new Content("").value()).isEmpty();
    assertThat(new Content("x".repeat(Content.MAX_LENGTH)).value()).hasSize(Content.MAX_LENGTH);
  }

  @Test
  @DisplayName("A content longer than its maximum is rejected")
  void contentLongerThanItsMaximumIsRejected() {
    // When / Then
    assertRejected(() -> new Content("x".repeat(Content.MAX_LENGTH + 1)));
  }

  @Test
  @DisplayName("An owner id accepts up to its maximum length")
  void ownerIdAcceptsUpToItsMaximumLength() {
    // When / Then
    assertThat(new OwnerId("x".repeat(OwnerId.MAX_LENGTH)).value()).hasSize(OwnerId.MAX_LENGTH);
  }

  @Test
  @DisplayName("A blank or oversized owner id is rejected")
  void blankOrOversizedOwnerIdIsRejected() {
    // When / Then
    assertRejected(() -> new OwnerId(" "));
    assertRejected(() -> new OwnerId("x".repeat(OwnerId.MAX_LENGTH + 1)));
  }

  private static void assertRejected(ThrowingCallable construction) {
    assertThatThrownBy(construction)
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception ->
                assertThat(exception.reason()).isEqualTo(InvalidRecordReason.CONSTRAINT_VIOLATION));
  }
}
