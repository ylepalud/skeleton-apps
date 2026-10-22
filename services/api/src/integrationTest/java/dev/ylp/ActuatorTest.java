package dev.ylp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "management.server.port=0")
@ImportTestcontainers(PostgresTestContainer.class)
class ActuatorTest implements MockOAuth2ServerContainer {

  @LocalServerPort private int serverPort;
  @LocalManagementPort private int managementPort;

  private final HttpClient client = HttpClient.newHttpClient();

  @AfterEach
  void tearDown() {
    client.close();
  }

  private HttpResponse<String> get(int port, String path) throws IOException, InterruptedException {
    return client.send(
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
        HttpResponse.BodyHandlers.ofString());
  }

  @Test
  @DisplayName("The probes answer on the management port without a token")
  void probesAnswerOnManagementPortWithoutToken() throws Exception {
    // When
    var liveness = get(managementPort, "/actuator/health/liveness");
    var readiness = get(managementPort, "/actuator/health/readiness");

    // Then
    assertThat(liveness.statusCode()).isEqualTo(200);
    assertThat(readiness.statusCode()).isEqualTo(200);
    assertThat(readiness.body()).contains("\"UP\"");
  }

  @Test
  @DisplayName("Metrics are exposed in the Prometheus format on the management port")
  void metricsAreExposedInPrometheusFormatOnManagementPort() throws Exception {
    // When
    var response = get(managementPort, "/actuator/prometheus");

    // Then
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("jvm_memory_used_bytes");
  }

  @Test
  @DisplayName("The build information is exposed on the management port")
  void buildInformationIsExposedOnManagementPort() throws Exception {
    // When
    var response = get(managementPort, "/actuator/info");

    // Then
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"build\"");
  }

  @Test
  @DisplayName("Actuator is not served on the application port")
  void actuatorIsNotServedOnApplicationPort() throws Exception {
    // When
    var response = get(serverPort, "/actuator/health");

    // Then
    assertThat(response.statusCode()).isNotEqualTo(200);
  }

  @Test
  @DisplayName("Any application route without a token is rejected")
  void anyApplicationRouteWithoutTokenIsRejected() throws Exception {
    // When
    var response = get(serverPort, "/anything");

    // Then
    assertThat(response.statusCode()).isEqualTo(401);
    assertThat(response.headers().firstValue("WWW-Authenticate"))
        .hasValueSatisfying(header -> assertThat(header).startsWith("Bearer"));
  }
}
