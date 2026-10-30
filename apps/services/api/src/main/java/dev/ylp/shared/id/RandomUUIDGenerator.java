package dev.ylp.shared.id;

import java.util.UUID;

/** Generates random (version 4) identifiers. */
public final class RandomUUIDGenerator implements UUIDGenerator {

  @Override
  public UUID generate() {
    return UUID.randomUUID();
  }
}
