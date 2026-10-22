package dev.ylp.notes.adapter.in.rest;

import dev.ylp.notes.adapter.in.rest.model.NoteDto;
import dev.ylp.notes.adapter.in.rest.model.NotePageDto;
import dev.ylp.notes.domain.Note;
import dev.ylp.notes.domain.Page;
import java.time.ZoneOffset;

/** Maps notes between the domain and the contract's transport objects. */
public final class NoteRestMapper {

  private NoteRestMapper() {}

  /** Returns the transport form of a note; times are expressed in UTC. */
  public static NoteDto toDto(Note note) {
    return new NoteDto(
        note.id().value(),
        note.title().value(),
        note.content().value(),
        note.createdAt().atOffset(ZoneOffset.UTC),
        note.updatedAt().atOffset(ZoneOffset.UTC));
  }

  /** Returns the transport form of a page of notes. */
  public static NotePageDto toDto(Page<Note> page) {
    return new NotePageDto(
        page.items().stream().map(NoteRestMapper::toDto).toList(),
        page.request().page(),
        page.request().size(),
        page.totalItems(),
        page.hasNext());
  }
}
