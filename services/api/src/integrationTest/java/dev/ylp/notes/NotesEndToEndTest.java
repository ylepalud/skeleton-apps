package dev.ylp.notes;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ylp.MockOAuth2ServerContainer;
import dev.ylp.PostgresTestContainer;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The whole chain, from a real token of the identity provider to PostgreSQL, through the HTTP
 * layer: one happy path, plus the isolation between two users.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ImportTestcontainers(PostgresTestContainer.class)
class NotesEndToEndTest implements MockOAuth2ServerContainer {

  @LocalServerPort private int port;

  private final HttpClient client = HttpClient.newHttpClient();
  private final JsonMapper json = JsonMapper.builder().build();

  @AfterEach
  void tearDown() {
    client.close();
  }

  private HttpResponse<String> send(String user, String method, String path, String body)
      throws IOException, InterruptedException {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Authorization", "Bearer " + MockOAuth2ServerContainer.accessTokenFor(user))
            .header("Content-Type", "application/json")
            .method(
                method, body.isEmpty() ? BodyPublishers.noBody() : BodyPublishers.ofString(body));
    return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }

  private JsonNode jsonOf(HttpResponse<String> response) {
    return json.readTree(response.body());
  }

  @Test
  @DisplayName("A user writes, lists, reads, edits and deletes a note that no one else can see")
  void userManagesNoteThatNoOneElseCanSee() throws Exception {
    // Create
    var created = send("alice", "POST", "/api/v1/notes", "{\"title\":\"Ideas\",\"content\":\"\"}");
    assertThat(created.statusCode()).isEqualTo(201);
    String location = created.headers().firstValue("Location").orElseThrow();
    assertThat(location).isEqualTo("/api/v1/notes/" + jsonOf(created).get("id").asString());

    // List
    var listed = send("alice", "GET", "/api/v1/notes", "");
    assertThat(listed.statusCode()).isEqualTo(200);
    assertThat(jsonOf(listed).get("items").get(0).get("title").asString()).isEqualTo("Ideas");

    // Read
    assertThat(send("alice", "GET", location, "").statusCode()).isEqualTo(200);

    // Isolation: another user cannot see, list, edit or delete it
    var stolen = send("bob", "GET", location, "");
    assertThat(stolen.statusCode()).isEqualTo(404);
    assertThat(jsonOf(stolen).get("errorCode").asString()).isEqualTo("NOTE_NOT_FOUND");
    assertThat(jsonOf(send("bob", "GET", "/api/v1/notes", "")).get("totalItems").asLong()).isZero();
    assertThat(send("bob", "PUT", location, "{\"title\":\"Mine\",\"content\":\"\"}").statusCode())
        .isEqualTo(404);
    assertThat(send("bob", "DELETE", location, "").statusCode()).isEqualTo(404);

    // Edit
    var edited = send("alice", "PUT", location, "{\"title\":\"Plans\",\"content\":\"Go\"}");
    assertThat(edited.statusCode()).isEqualTo(200);
    assertThat(jsonOf(edited).get("title").asString()).isEqualTo("Plans");

    // Delete
    assertThat(send("alice", "DELETE", location, "").statusCode()).isEqualTo(204);
    assertThat(send("alice", "GET", location, "").statusCode()).isEqualTo(404);
  }
}
