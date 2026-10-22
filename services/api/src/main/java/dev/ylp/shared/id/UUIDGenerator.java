package dev.ylp.shared.id;

import java.util.UUID;

/** Generates the identifier of every new entity, so that an entity never waits for the database. */
public interface UUIDGenerator {

  /** Returns a new identifier. */
  UUID generate();
}
