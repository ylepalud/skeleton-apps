package dev.ylp.notes.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NoteErrorReasonTest {

  @Test
  @DisplayName("A missing note is reported with its id and an actionable hint")
  void missingNoteIsReportedWithItsIdAndActionableHint() {
    // Given
    NoteId id = new NoteId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    // When
    var exception = NoteErrorReason.noteNotFound(id);

    // Then
    assertThat(exception.reason()).isEqualTo(NoteErrorReason.NOTE_NOT_FOUND);
    assertThat(exception).hasMessage("Note not found: 00000000-0000-0000-0000-000000000001");
    assertThat(exception.hint()).isNotBlank();
  }
}
