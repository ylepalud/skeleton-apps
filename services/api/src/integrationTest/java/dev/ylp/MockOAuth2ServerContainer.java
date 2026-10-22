package dev.ylp;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import tools.jackson.databind.json.JsonMapper;

/**
 * A single mock OpenID Connect provider for the whole test JVM, started once and never stopped. Any
 * client id gets a token whose subject is that client id, addressed to the {@code api} audience.
 * Test classes import it with {@code @ImportTestcontainers}.
 */
public interface MockOAuth2ServerContainer {

  String IMAGE =
      "ghcr.io/navikt/mock-oauth2-server:6.0.4"
          + "@sha256:47374fe063995d9b6d39ebb6ea21eb5f765025bf0aa08b61b08b439eb91adaf0";

  String CONFIG =
      """
      {"tokenCallbacks": [{"issuerId": "default", "tokenExpiry": 3600, "requestMappings": [
        {"requestParam": "grant_type", "match": "client_credentials",
         "claims": {"sub": "${clientId}", "aud": ["api"]}}]}]}
      """;

  GenericContainer<?> OAUTH2 = startOAuth2();

  private static GenericContainer<?> startOAuth2() {
    GenericContainer<?> container =
        new GenericContainer<>(IMAGE)
            .withExposedPorts(8080)
            .withEnv("JSON_CONFIG", CONFIG)
            .waitingFor(Wait.forHttp("/default/.well-known/openid-configuration"));
    container.start();
    return container;
  }

  /** Returns the issuer the API trusts, as reachable from the test JVM. */
  static String issuer() {
    return "http://" + OAUTH2.getHost() + ":" + OAUTH2.getMappedPort(8080) + "/default";
  }

  /** Points the API at the mock provider. */
  @DynamicPropertySource
  static void oauth2Properties(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.security.oauth2.resourceserver.jwt.issuer-uri", MockOAuth2ServerContainer::issuer);
  }

  /** Returns a fresh access token whose subject is the given user. */
  static String accessTokenFor(String subject) {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(issuer() + "/token"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    "grant_type=client_credentials&client_secret=unused&client_id=" + subject))
            .build();
    try (HttpClient client = HttpClient.newHttpClient()) {
      String body = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
      return JsonMapper.builder().build().readTree(body).get("access_token").asString();
    } catch (IOException e) {
      throw new IllegalStateException("Could not get a token from the mock provider", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while getting a token", e);
    }
  }
}
