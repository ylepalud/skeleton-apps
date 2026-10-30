package dev.ylp.notes.adapter.out.postgres;

import dev.ylp.PostgresTestContainer;
import dev.ylp.notes.domain.NoteRepository;
import dev.ylp.notes.domain.NoteRepositoryContractTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportTestcontainers(PostgresTestContainer.class)
@Import(PostgresNoteRepository.class)
class PostgresNoteRepositoryTest extends NoteRepositoryContractTest {

  @Autowired private PostgresNoteRepository repository;

  @Override
  protected NoteRepository repository() {
    return repository;
  }
}
