package dev.ylp.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

class RequestSizeLimitFilterTest {

  private final RequestSizeLimitFilter filter =
      new RequestSizeLimitFilter(JsonMapper.builder().build());

  private static MockHttpServletRequest requestWithBodyOf(long bytes) {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/notes");
    request.setContent(new byte[Math.toIntExact(bytes)]);
    return request;
  }

  @Test
  @DisplayName("A body up to the limit goes through")
  void bodyUpToTheLimitGoesThrough() throws Exception {
    // Given
    var chain = new MockFilterChain();

    // When
    filter.doFilter(
        requestWithBodyOf(RequestSizeLimitFilter.MAX_BODY_BYTES),
        new MockHttpServletResponse(),
        chain);

    // Then
    assertThat(chain.getRequest()).isNotNull();
  }

  @Test
  @DisplayName("A body over the limit is rejected with a 413 problem, without going further")
  void bodyOverTheLimitIsRejectedWithA413ProblemWithoutGoingFurther() throws Exception {
    // Given
    var chain = new MockFilterChain();
    var response = new MockHttpServletResponse();

    // When
    filter.doFilter(requestWithBodyOf(RequestSizeLimitFilter.MAX_BODY_BYTES + 1), response, chain);

    // Then
    assertThat(chain.getRequest()).isNull();
    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(response.getContentType()).isEqualTo("application/problem+json");
    assertThat(response.getContentAsString())
        .contains("\"status\":413")
        .contains("\"instance\":\"/api/v1/notes\"");
  }
}
