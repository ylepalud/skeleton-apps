package dev.ylp.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ylp.shared.error.business.BusinessErrorReason;
import dev.ylp.shared.error.business.BusinessException;
import dev.ylp.shared.error.business.ErrorCategory;
import dev.ylp.shared.error.business.InvalidRecordReason;
import dev.ylp.shared.error.technical.GenericTechnicalReason;
import dev.ylp.shared.error.technical.TechnicalException;
import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class ProblemDetailsExceptionHandlerTest {

  private final ProblemDetailsExceptionHandler handler = new ProblemDetailsExceptionHandler();
  private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/x");

  private enum SampleReason implements BusinessErrorReason {
    BROKEN_RULE;

    @Override
    public String hint() {
      return "Do something else.";
    }
  }

  @ParameterizedTest
  @CsvSource({
    "NOT_FOUND, NOT_FOUND",
    "INVALID, BAD_REQUEST",
    "CONFLICT, CONFLICT",
    "RULE_VIOLATION, UNPROCESSABLE_CONTENT"
  })
  @DisplayName("Each error category maps to its HTTP status")
  void eachErrorCategoryMapsToItsHttpStatus(ErrorCategory category, HttpStatus status) {
    // When / Then
    assertThat(ProblemDetailsExceptionHandler.statusOf(category)).isEqualTo(status);
  }

  @Test
  @DisplayName("A business error becomes a problem carrying its code, hint and path")
  void businessErrorBecomesProblemCarryingItsCodeHintAndPath() {
    // Given
    var exception = new BusinessException(InvalidRecordReason.CONSTRAINT_VIOLATION, "Too long");

    // When
    ResponseEntity<ProblemDetail> response = handler.handleBusiness(exception, request);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    ProblemDetail problem = response.getBody();
    assertThat(problem).isNotNull();
    assertThat(problem.getType()).isEqualTo(URI.create("urn:problem:constraint_violation"));
    assertThat(problem.getDetail()).isEqualTo("Too long");
    assertThat(problem.getInstance()).isEqualTo(URI.create("/api/v1/x"));
    assertThat(problem.getProperties())
        .containsEntry("errorCode", "CONSTRAINT_VIOLATION")
        .containsEntry("hint", InvalidRecordReason.CONSTRAINT_VIOLATION.hint());
  }

  @Test
  @DisplayName("A business rule without a category is a 422")
  void businessRuleWithoutCategoryIsA422() {
    // When
    var response =
        handler.handleBusiness(new BusinessException(SampleReason.BROKEN_RULE, "No"), request);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
  }

  @Test
  @DisplayName("A technical error becomes a 503 that hides its internal message")
  void technicalErrorBecomesA503ThatHidesItsInternalMessage() {
    // Given
    var exception =
        new TechnicalException(
            GenericTechnicalReason.INFRASTRUCTURE_FAILURE,
            "jdbc:postgresql://db:5432 refused",
            new IllegalStateException());

    // When
    ResponseEntity<ProblemDetail> response = handler.handleTechnical(exception, request);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    ProblemDetail problem = response.getBody();
    assertThat(problem).isNotNull();
    assertThat(problem.getDetail()).doesNotContain("jdbc");
    assertThat(problem.getProperties()).containsEntry("errorCode", "INFRASTRUCTURE_FAILURE");
  }

  @Test
  @DisplayName("An unexpected error becomes a 500 that reveals nothing")
  void unexpectedErrorBecomesA500ThatRevealsNothing() {
    // When
    ResponseEntity<ProblemDetail> response =
        handler.handleUnexpected(new IllegalStateException("secret internals"), request);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    ProblemDetail problem = response.getBody();
    assertThat(problem).isNotNull();
    assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred.");
    assertThat(problem.getProperties()).isNull();
  }
}
