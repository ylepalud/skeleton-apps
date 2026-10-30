package dev.ylp.notes;

import dev.ylp.notes.application.CreateNoteUseCase;
import dev.ylp.notes.application.DeleteNoteUseCase;
import dev.ylp.notes.application.GetNoteUseCase;
import dev.ylp.notes.application.ListNotesUseCase;
import dev.ylp.notes.application.UpdateNoteUseCase;
import dev.ylp.notes.domain.NoteRepository;
import dev.ylp.shared.id.UUIDGenerator;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Declares the use cases explicitly, so that the application layer carries no Spring stereotype
// (@Transactional is the only Spring annotation it uses).
@Configuration
class NotesConfiguration {

  @Bean
  CreateNoteUseCase createNoteUseCase(
      NoteRepository noteRepository, UUIDGenerator uuidGenerator, Clock clock) {
    return new CreateNoteUseCase(noteRepository, uuidGenerator, clock);
  }

  @Bean
  ListNotesUseCase listNotesUseCase(NoteRepository noteRepository) {
    return new ListNotesUseCase(noteRepository);
  }

  @Bean
  GetNoteUseCase getNoteUseCase(NoteRepository noteRepository) {
    return new GetNoteUseCase(noteRepository);
  }

  @Bean
  UpdateNoteUseCase updateNoteUseCase(NoteRepository noteRepository, Clock clock) {
    return new UpdateNoteUseCase(noteRepository, clock);
  }

  @Bean
  DeleteNoteUseCase deleteNoteUseCase(NoteRepository noteRepository) {
    return new DeleteNoteUseCase(noteRepository);
  }
}
