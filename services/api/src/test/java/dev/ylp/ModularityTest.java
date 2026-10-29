package dev.ylp;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

  private final ApplicationModules modules = ApplicationModules.of(ApiApplication.class);

  @Test
  @DisplayName("Modules only depend on each other through their exposed API")
  void modulesOnlyDependOnEachOtherThroughTheirExposedApi() {
    // When / Then
    modules.verify();
  }

  @Test
  @DisplayName("The domain model page shows each module's model and request flow")
  void domainModelPageShowsEachModulesModelAndRequestFlow() {
    // Given: docsDomainModel points the page at the documentation site
    Path file =
        Path.of(System.getProperty("domainModel.page", "build/domain-model/domain-model.md"));

    // When
    new DomainModelPage(modules, Path.of("src/main/java")).writeTo(file);

    // Then
    assertThat(file)
        .content()
        .contains("notes --> shared")
        .contains("<<aggregate root>>", "<<value object>>", "<<secondary port>>")
        .contains("Note *-- Title : title", "Note *-- NoteId : id", "NoteRepository ..> Note")
        .contains("NotesController --> CreateNoteUseCase", "CreateNoteUseCase --> NoteRepository")
        .contains("PostgresNoteRepository -. implements .-> NoteRepository")
        .containsSubsequence("## Business modules", "### Notes", "## Cross-cutting modules")
        .containsSubsequence("## Cross-cutting modules", "### Config", "### Shared")
        .contains("Used by: Config, Notes.");
  }
}
