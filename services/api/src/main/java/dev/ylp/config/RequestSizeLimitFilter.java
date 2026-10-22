package dev.ylp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Rejects a request body larger than {@value #MAX_BODY_BYTES} bytes with a 413 problem, before
 * authentication and before the body is read. The longest valid note fits well below it.
 *
 * <p>Relies on the declared {@code Content-Length}: a chunked body without one is not covered.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestSizeLimitFilter extends OncePerRequestFilter {

  /** Largest accepted request body: 64 KiB. */
  public static final long MAX_BODY_BYTES = 64 * 1024;

  private final JsonMapper jsonMapper;

  /** Creates the filter. */
  public RequestSizeLimitFilter(JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.getContentLengthLong() > MAX_BODY_BYTES) {
      ProblemDetail problem =
          ProblemDetail.forStatusAndDetail(
              HttpStatus.CONTENT_TOO_LARGE,
              "The request body exceeds " + MAX_BODY_BYTES + " bytes.");
      problem.setInstance(URI.create(request.getRequestURI()));
      response.setStatus(HttpStatus.CONTENT_TOO_LARGE.value());
      response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
      jsonMapper.writeValue(response.getOutputStream(), problem);
      return;
    }
    chain.doFilter(request, response);
  }
}
