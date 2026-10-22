package dev.ylp;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * A single PostgreSQL container for the whole test JVM, started once and never stopped. Test
 * classes import it with {@code @ImportTestcontainers(PostgresTestContainer.class)}.
 */
public interface PostgresTestContainer {

  @ServiceConnection PostgreSQLContainer POSTGRES = startPostgres();

  private static PostgreSQLContainer startPostgres() {
    // An interface cannot hold a static initializer block: this helper plays that role.
    PostgreSQLContainer container = new PostgreSQLContainer("postgres:18-alpine");
    container.start();
    return container;
  }
}
