package dev.ylp.notes.domain;

import java.util.UUID;
import org.jmolecules.ddd.annotation.ValueObject;

/** Identity of a {@link Note}. */
@ValueObject
public record NoteId(UUID value) {}
