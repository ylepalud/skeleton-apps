package dev.ylp.notes.adapter.in.rest;

import static dev.ylp.notes.domain.NoteTestDataBuilder.CREATED_AT;
import static dev.ylp.notes.domain.NoteTestDataBuilder.aNote;
import static org.assertj.core.api.Assertions.assertThat;

import dev.ylp.notes.adapter.in.rest.model.NoteDto;
import dev.ylp.notes.adapter.in.rest.model.NotePageDto;
import dev.ylp.notes.domain.Note;
import dev.ylp.notes.domain.Page;
import dev.ylp.notes.domain.PageRequest;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NoteRestMapperTest {

  @Test
  @DisplayName("A note keeps every field, with its times in UTC")
  void noteKeepsEveryFieldWithItsTimesInUtc() {
    // Given
    Note note = aNote().withTitle("Ideas").withUpdatedAt(CREATED_AT.plusSeconds(60)).build();

    // When
    NoteDto dto = NoteRestMapper.toDto(note);

    // Then
    assertThat(dto.getId()).isEqualTo(note.id().value());
    assertThat(dto.getTitle()).isEqualTo("Ideas");
    assertThat(dto.getContent()).isEqualTo(note.content().value());
    assertThat(dto.getCreatedAt()).isEqualTo(CREATED_AT.atOffset(ZoneOffset.UTC));
    assertThat(dto.getUpdatedAt()).isEqualTo(CREATED_AT.plusSeconds(60).atOffset(ZoneOffset.UTC));
  }

  @Test
  @DisplayName("A page keeps its items, position, total and whether more follow")
  void pageKeepsItsItemsPositionTotalAndWhetherMoreFollow() {
    // Given
    Note note = aNote().build();
    Page<Note> page = new Page<>(List.of(note), new PageRequest(1, 1), 3);

    // When
    NotePageDto dto = NoteRestMapper.toDto(page);

    // Then
    assertThat(dto.getItems()).extracting(NoteDto::getId).containsExactly(note.id().value());
    assertThat(dto.getPage()).isEqualTo(1);
    assertThat(dto.getSize()).isEqualTo(1);
    assertThat(dto.getTotalItems()).isEqualTo(3);
    assertThat(dto.getHasNext()).isTrue();
  }
}
