package dev.ylp.notes.application;

import static dev.ylp.notes.domain.NoteTestDataBuilder.CREATED_AT;
import static dev.ylp.notes.domain.NoteTestDataBuilder.aNote;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ylp.notes.application.CreateNoteUseCase.CreateNoteCommand;
import dev.ylp.notes.application.DeleteNoteUseCase.DeleteNoteCommand;
import dev.ylp.notes.application.GetNoteUseCase.GetNoteCommand;
import dev.ylp.notes.application.ListNotesUseCase.ListNotesCommand;
import dev.ylp.notes.application.UpdateNoteUseCase.UpdateNoteCommand;
import dev.ylp.notes.domain.Content;
import dev.ylp.notes.domain.Note;
import dev.ylp.notes.domain.NoteErrorReason;
import dev.ylp.notes.domain.NoteId;
import dev.ylp.notes.domain.NoteRepository;
import dev.ylp.notes.domain.OwnerId;
import dev.ylp.notes.domain.Page;
import dev.ylp.notes.domain.PageRequest;
import dev.ylp.notes.domain.Title;
import dev.ylp.shared.error.business.BusinessException;
import dev.ylp.shared.id.UUIDGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoteUseCasesTest {

  private static final Instant NOW = CREATED_AT.plusSeconds(3_600);
  private static final OwnerId OWNER = new OwnerId("ada");

  @Mock private NoteRepository noteRepository;
  @Mock private UUIDGenerator uuidGenerator;

  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private Note stored;

  @BeforeEach
  void setUp() {
    stored = aNote().withOwner(OWNER).build();
  }

  private void givenNoStoredNote(NoteId id) {
    when(noteRepository.findByIdAndOwner(id, OWNER)).thenReturn(Optional.empty());
  }

  private void givenStored(Note note) {
    when(noteRepository.findByIdAndOwner(note.id(), OWNER)).thenReturn(Optional.of(note));
  }

  private static void assertNotFound(BusinessException exception) {
    assertThat(exception.reason()).isEqualTo(NoteErrorReason.NOTE_NOT_FOUND);
  }

  @Nested
  class Create {

    @Test
    @DisplayName("Creating a note stores it with a generated id and returns it")
    void creatingNoteStoresItWithGeneratedIdAndReturnsIt() {
      // Given
      UUID generated = UUID.randomUUID();
      when(uuidGenerator.generate()).thenReturn(generated);
      var useCase = new CreateNoteUseCase(noteRepository, uuidGenerator, clock);

      // When
      Note note =
          useCase.execute(new CreateNoteCommand(OWNER, new Title("Ideas"), new Content("Text")));

      // Then
      assertThat(note.id()).isEqualTo(new NoteId(generated));
      assertThat(note.owner()).isEqualTo(OWNER);
      assertThat(note.createdAt()).isEqualTo(NOW);
      verify(noteRepository).save(note);
    }
  }

  @Nested
  class List_ {

    @Test
    @DisplayName("Listing returns the owner's requested page")
    void listingReturnsOwnersRequestedPage() {
      // Given
      PageRequest request = new PageRequest(0, 20);
      Page<Note> page = new Page<>(List.of(stored), request, 1);
      when(noteRepository.findByOwner(OWNER, request)).thenReturn(page);

      // When
      Page<Note> result =
          new ListNotesUseCase(noteRepository).execute(new ListNotesCommand(OWNER, request));

      // Then
      assertThat(result).isEqualTo(page);
    }
  }

  @Nested
  class Get {

    @Test
    @DisplayName("Reading an owned note returns it")
    void readingOwnedNoteReturnsIt() {
      // Given
      givenStored(stored);

      // When
      Note note =
          new GetNoteUseCase(noteRepository).execute(new GetNoteCommand(OWNER, stored.id()));

      // Then
      assertThat(note).isSameAs(stored);
    }

    @Test
    @DisplayName("Reading a note that is missing or not owned fails as not found")
    void readingNoteMissingOrNotOwnedFailsAsNotFound() {
      // Given
      NoteId id = new NoteId(UUID.randomUUID());
      givenNoStoredNote(id);
      var useCase = new GetNoteUseCase(noteRepository);

      // When / Then
      assertThatThrownBy(() -> useCase.execute(new GetNoteCommand(OWNER, id)))
          .isInstanceOfSatisfying(BusinessException.class, NoteUseCasesTest::assertNotFound);
    }
  }

  @Nested
  class Update {

    @Test
    @DisplayName("Updating an owned note edits it, stores it and returns it")
    void updatingOwnedNoteEditsItStoresItAndReturnsIt() {
      // Given
      givenStored(stored);
      var useCase = new UpdateNoteUseCase(noteRepository, clock);

      // When
      Note note =
          useCase.execute(
              new UpdateNoteCommand(OWNER, stored.id(), new Title("New"), new Content("Body")));

      // Then
      assertThat(note.title()).isEqualTo(new Title("New"));
      assertThat(note.content()).isEqualTo(new Content("Body"));
      assertThat(note.updatedAt()).isEqualTo(NOW);
      verify(noteRepository).save(stored);
    }

    @Test
    @DisplayName("Updating a note that is missing or not owned fails and stores nothing")
    void updatingNoteMissingOrNotOwnedFailsAndStoresNothing() {
      // Given
      NoteId id = new NoteId(UUID.randomUUID());
      givenNoStoredNote(id);
      var useCase = new UpdateNoteUseCase(noteRepository, clock);
      var command = new UpdateNoteCommand(OWNER, id, new Title("New"), new Content(""));

      // When / Then
      assertThatThrownBy(() -> useCase.execute(command))
          .isInstanceOfSatisfying(BusinessException.class, NoteUseCasesTest::assertNotFound);
      verify(noteRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
  }

  @Nested
  class Delete {

    @Test
    @DisplayName("Deleting an owned note removes it")
    void deletingOwnedNoteRemovesIt() {
      // Given
      givenStored(stored);

      // When
      new DeleteNoteUseCase(noteRepository).execute(new DeleteNoteCommand(OWNER, stored.id()));

      // Then
      verify(noteRepository).delete(stored);
    }

    @Test
    @DisplayName("Deleting a note that is missing or not owned fails and removes nothing")
    void deletingNoteMissingOrNotOwnedFailsAndRemovesNothing() {
      // Given
      NoteId id = new NoteId(UUID.randomUUID());
      givenNoStoredNote(id);
      var useCase = new DeleteNoteUseCase(noteRepository);

      // When / Then
      assertThatThrownBy(() -> useCase.execute(new DeleteNoteCommand(OWNER, id)))
          .isInstanceOfSatisfying(BusinessException.class, NoteUseCasesTest::assertNotFound);
      verify(noteRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }
  }
}
