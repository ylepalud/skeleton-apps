package dev.ylp.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ylp.shared.error.business.BusinessException;
import dev.ylp.shared.error.business.InvalidRecordReason;
import dev.ylp.shared.error.technical.GenericTechnicalReason;
import dev.ylp.shared.error.technical.TechnicalException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExceptionsTest {

  @Test
  @DisplayName("A business exception exposes the code and hint of its reason")
  void businessExceptionExposesCodeAndHintOfItsReason() {
    // When
    var exception = new BusinessException(InvalidRecordReason.CONSTRAINT_VIOLATION, "bad value");

    // Then
    assertThat(exception.reason()).isEqualTo(InvalidRecordReason.CONSTRAINT_VIOLATION);
    assertThat(exception.errorCode()).isEqualTo("CONSTRAINT_VIOLATION");
    assertThat(exception.hint()).isEqualTo(InvalidRecordReason.CONSTRAINT_VIOLATION.hint());
    assertThat(exception).hasMessage("bad value");
  }

  @Test
  @DisplayName("A technical exception exposes its reason and keeps its cause")
  void technicalExceptionExposesItsReasonAndKeepsItsCause() {
    // Given
    var cause = new IllegalStateException("connection refused");

    // When
    var exception =
        new TechnicalException(GenericTechnicalReason.INFRASTRUCTURE_FAILURE, "db down", cause);

    // Then
    assertThat(exception.reason()).isEqualTo(GenericTechnicalReason.INFRASTRUCTURE_FAILURE);
    assertThat(exception.errorCode()).isEqualTo("INFRASTRUCTURE_FAILURE");
    assertThat(exception.hint()).isEqualTo(GenericTechnicalReason.INFRASTRUCTURE_FAILURE.hint());
    assertThat(exception).hasMessage("db down").hasCause(cause);
  }
}
