package dev.ylp;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ylp.notes.application.CreateNoteUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;

@SpringBootTest
@ImportTestcontainers(PostgresTestContainer.class)
class ApiApplicationBootTest implements MockOAuth2ServerContainer {

  @Autowired private CreateNoteUseCase createNoteUseCase;

  @Test
  @DisplayName("The application starts against a real PostgreSQL migrated by Liquibase")
  void applicationStartsAgainstRealPostgresqlMigratedByLiquibase() {
    // Then: the context, the migrations and the Hibernate schema validation all succeeded
    assertThat(createNoteUseCase).isNotNull();
  }
}
