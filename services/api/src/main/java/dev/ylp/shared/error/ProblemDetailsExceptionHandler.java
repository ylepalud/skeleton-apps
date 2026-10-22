package dev.ylp.shared.error;

import dev.ylp.shared.error.business.BusinessException;
import dev.ylp.shared.error.business.ErrorCategory;
import dev.ylp.shared.error.technical.TechnicalException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every error into an RFC 9457 problem. Business and technical errors carry their stable
 * {@code errorCode} and {@code hint}; Spring MVC's own errors (validation, unreadable body) keep
 * the problems built by {@link ResponseEntityExceptionHandler}; anything else becomes a 500 that
 * reveals nothing about the cause.
 */
@RestControllerAdvice
public class ProblemDetailsExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ProblemDetailsExceptionHandler.class);

  /** Answers a broken business rule with the status matching its category. */
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ProblemDetail> handleBusiness(
      BusinessException exception, HttpServletRequest request) {
    log.warn("business_error errorCode={}", exception.errorCode());
    HttpStatus status = statusOf(exception.reason().category());
    return problem(
        status,
        exception.getMessage(),
        exception.errorCode(),
        exception.hint(),
        request.getRequestURI());
  }

  /** Answers an infrastructure failure with a 503, without exposing its internal message. */
  @ExceptionHandler(TechnicalException.class)
  public ResponseEntity<ProblemDetail> handleTechnical(
      TechnicalException exception, HttpServletRequest request) {
    log.error("technical_error errorCode={}", exception.errorCode(), exception);
    return problem(
        HttpStatus.SERVICE_UNAVAILABLE,
        "A dependency of the service is unavailable.",
        exception.errorCode(),
        exception.hint(),
        request.getRequestURI());
  }

  /** Answers any unexpected error with a 500 that reveals nothing about the cause. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleUnexpected(
      Exception exception, HttpServletRequest request) {
    log.error("unexpected_error", exception);
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    problem.setInstance(URI.create(request.getRequestURI()));
    return respond(HttpStatus.INTERNAL_SERVER_ERROR, problem);
  }

  static HttpStatus statusOf(ErrorCategory category) {
    return switch (category) {
      case NOT_FOUND -> HttpStatus.NOT_FOUND;
      case INVALID -> HttpStatus.BAD_REQUEST;
      case CONFLICT -> HttpStatus.CONFLICT;
      case RULE_VIOLATION -> HttpStatus.UNPROCESSABLE_CONTENT;
    };
  }

  private static ResponseEntity<ProblemDetail> problem(
      HttpStatus status, @Nullable String detail, String errorCode, String hint, String path) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setType(URI.create("urn:problem:" + errorCode.toLowerCase(Locale.ROOT)));
    problem.setInstance(URI.create(path));
    problem.setProperty("errorCode", errorCode);
    problem.setProperty("hint", hint);
    return respond(status, problem);
  }

  private static ResponseEntity<ProblemDetail> respond(HttpStatus status, ProblemDetail problem) {
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }
}
