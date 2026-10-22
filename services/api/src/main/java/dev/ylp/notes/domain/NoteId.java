package dev.ylp.notes.domain;

import java.util.UUID;

/** Identity of a {@link Note}. */
public record NoteId(UUID value) {}
