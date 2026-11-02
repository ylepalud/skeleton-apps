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
import org.jspecify.annotations.Nullable;
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
 * layer: one happy path, the isolation between two users, and concurrent changes.
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
    return send(user, method, path, body, null);
  }

  private HttpResponse<String> send(
      String user, String method, String path, String body, @Nullable String ifMatch)
      throws IOException, InterruptedException {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Authorization", "Bearer " + MockOAuth2ServerContainer.accessTokenFor(user))
            .header("Content-Type", "application/json")
            .method(
                method, body.isEmpty() ? BodyPublishers.noBody() : BodyPublishers.ofString(body));
    if (ifMatch != null) {
      request.header("If-Match", ifMatch);
    }
    return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }

  private static String etagOf(HttpResponse<String> response) {
    return response.headers().firstValue("ETag").orElseThrow();
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
    var read = send("alice", "GET", location, "");
    assertThat(read.statusCode()).isEqualTo(200);
    assertThat(etagOf(read)).isEqualTo("\"0\"").isEqualTo(etagOf(created));

    // Isolation: another user cannot see, list, edit or delete it
    var stolen = send("bob", "GET", location, "");
    assertThat(stolen.statusCode()).isEqualTo(404);
    assertThat(jsonOf(stolen).get("errorCode").asString()).isEqualTo("NOTE_NOT_FOUND");
    assertThat(jsonOf(send("bob", "GET", "/api/v1/notes", "")).get("totalItems").asLong()).isZero();
    assertThat(
            send("bob", "PUT", location, "{\"title\":\"Mine\",\"content\":\"\"}", "*").statusCode())
        .isEqualTo(404);
    assertThat(send("bob", "DELETE", location, "", "*").statusCode()).isEqualTo(404);

    // Edit
    var edited =
        send("alice", "PUT", location, "{\"title\":\"Plans\",\"content\":\"Go\"}", etagOf(read));
    assertThat(edited.statusCode()).isEqualTo(200);
    assertThat(jsonOf(edited).get("title").asString()).isEqualTo("Plans");
    assertThat(jsonOf(edited).get("version").asLong()).isEqualTo(1);
    assertThat(etagOf(edited)).isEqualTo("\"1\"");

    // Delete
    assertThat(send("alice", "DELETE", location, "", etagOf(edited)).statusCode()).isEqualTo(204);
    assertThat(send("alice", "GET", location, "").statusCode()).isEqualTo(404);
  }

  @Test
  @DisplayName("A change based on an older version answers 412 and leaves the note unchanged")
  void changeBasedOnOlderVersionAnswers412AndLeavesNoteUnchanged() throws Exception {
    // Given: two copies read at version 0, the first one saved
    var created = send("carol", "POST", "/api/v1/notes", "{\"title\":\"Ideas\",\"content\":\"\"}");
    String location = created.headers().firstValue("Location").orElseThrow();
    String readByBoth = etagOf(created);
    var first =
        send("carol", "PUT", location, "{\"title\":\"First\",\"content\":\"\"}", readByBoth);
    assertThat(first.statusCode()).isEqualTo(200);

    // When: the second copy is saved, then deleted
    var second =
        send("carol", "PUT", location, "{\"title\":\"Second\",\"content\":\"\"}", readByBoth);
    var deleted = send("carol", "DELETE", location, "", readByBoth);

    // Then
    assertThat(second.statusCode()).isEqualTo(412);
    assertThat(jsonOf(second).get("errorCode").asString()).isEqualTo("VERSION_MISMATCH");
    assertThat(deleted.statusCode()).isEqualTo(412);
    var stored = send("carol", "GET", location, "");
    assertThat(jsonOf(stored).get("title").asString()).isEqualTo("First");
    assertThat(etagOf(stored)).isEqualTo(etagOf(first));
  }

  @Test
  @DisplayName("A change without If-Match answers 428 and leaves the note unchanged")
  void changeWithoutIfMatchAnswers428AndLeavesNoteUnchanged() throws Exception {
    // Given
    var created = send("dave", "POST", "/api/v1/notes", "{\"title\":\"Ideas\",\"content\":\"\"}");
    String location = created.headers().firstValue("Location").orElseThrow();

    // When
    var edited = send("dave", "PUT", location, "{\"title\":\"Blind\",\"content\":\"\"}");
    var deleted = send("dave", "DELETE", location, "");

    // Then
    assertThat(edited.statusCode()).isEqualTo(428);
    assertThat(jsonOf(edited).get("errorCode").asString()).isEqualTo("VERSION_REQUIRED");
    assertThat(deleted.statusCode()).isEqualTo(428);
    assertThat(jsonOf(send("dave", "GET", location, "")).get("title").asString())
        .isEqualTo("Ideas");
  }
}
