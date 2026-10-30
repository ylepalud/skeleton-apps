package dev.ylp.notes.adapter.in.rest;

import dev.ylp.notes.adapter.in.rest.api.NotesApi;
import dev.ylp.notes.adapter.in.rest.model.NoteDto;
import dev.ylp.notes.adapter.in.rest.model.NotePageDto;
import dev.ylp.notes.adapter.in.rest.model.NoteRequestDto;
import dev.ylp.notes.application.CreateNoteUseCase;
import dev.ylp.notes.application.CreateNoteUseCase.CreateNoteCommand;
import dev.ylp.notes.application.DeleteNoteUseCase;
import dev.ylp.notes.application.DeleteNoteUseCase.DeleteNoteCommand;
import dev.ylp.notes.application.GetNoteUseCase;
import dev.ylp.notes.application.GetNoteUseCase.GetNoteCommand;
import dev.ylp.notes.application.ListNotesUseCase;
import dev.ylp.notes.application.ListNotesUseCase.ListNotesCommand;
import dev.ylp.notes.application.UpdateNoteUseCase;
import dev.ylp.notes.application.UpdateNoteUseCase.UpdateNoteCommand;
import dev.ylp.notes.domain.Content;
import dev.ylp.notes.domain.Note;
import dev.ylp.notes.domain.NoteId;
import dev.ylp.notes.domain.PageRequest;
import dev.ylp.notes.domain.Title;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Serves the notes operations of the contract: builds each command and calls its use case. */
@RestController
public class NotesController implements NotesApi {

  private final CreateNoteUseCase createNoteUseCase;
  private final ListNotesUseCase listNotesUseCase;
  private final GetNoteUseCase getNoteUseCase;
  private final UpdateNoteUseCase updateNoteUseCase;
  private final DeleteNoteUseCase deleteNoteUseCase;
  private final CurrentOwner currentOwner;

  /** Creates the controller. */
  public NotesController(
      CreateNoteUseCase createNoteUseCase,
      ListNotesUseCase listNotesUseCase,
      GetNoteUseCase getNoteUseCase,
      UpdateNoteUseCase updateNoteUseCase,
      DeleteNoteUseCase deleteNoteUseCase,
      CurrentOwner currentOwner) {
    this.createNoteUseCase = createNoteUseCase;
    this.listNotesUseCase = listNotesUseCase;
    this.getNoteUseCase = getNoteUseCase;
    this.updateNoteUseCase = updateNoteUseCase;
    this.deleteNoteUseCase = deleteNoteUseCase;
    this.currentOwner = currentOwner;
  }

  @Override
  public ResponseEntity<NotePageDto> listNotes(Integer page, Integer size) {
    var command = new ListNotesCommand(currentOwner.get(), new PageRequest(page, size));
    return ResponseEntity.ok(NoteRestMapper.toDto(listNotesUseCase.execute(command)));
  }

  @Override
  public ResponseEntity<NoteDto> createNote(NoteRequestDto request) {
    Note note =
        createNoteUseCase.execute(
            new CreateNoteCommand(
                currentOwner.get(),
                new Title(request.getTitle()),
                new Content(request.getContent())));
    // A path-only location: the public host and scheme belong to the edge, not to this service.
    String path =
        ServletUriComponentsBuilder.fromCurrentRequestUri()
            .path("/{noteId}")
            .buildAndExpand(note.id().value())
            .getPath();
    return ResponseEntity.created(URI.create(String.valueOf(path)))
        .body(NoteRestMapper.toDto(note));
  }

  @Override
  public ResponseEntity<NoteDto> getNote(UUID noteId) {
    Note note = getNoteUseCase.execute(new GetNoteCommand(currentOwner.get(), new NoteId(noteId)));
    return ResponseEntity.ok(NoteRestMapper.toDto(note));
  }

  @Override
  public ResponseEntity<NoteDto> updateNote(UUID noteId, NoteRequestDto request) {
    Note note =
        updateNoteUseCase.execute(
            new UpdateNoteCommand(
                currentOwner.get(),
                new NoteId(noteId),
                new Title(request.getTitle()),
                new Content(request.getContent())));
    return ResponseEntity.ok(NoteRestMapper.toDto(note));
  }

  @Override
  public ResponseEntity<Void> deleteNote(UUID noteId) {
    deleteNoteUseCase.execute(new DeleteNoteCommand(currentOwner.get(), new NoteId(noteId)));
    return ResponseEntity.noContent().build();
  }
}
