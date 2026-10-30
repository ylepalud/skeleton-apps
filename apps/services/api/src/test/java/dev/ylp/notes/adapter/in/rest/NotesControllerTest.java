package dev.ylp.notes.adapter.in.rest;

import static dev.ylp.notes.domain.NoteTestDataBuilder.aNote;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.ylp.config.SecurityConfiguration;
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
import dev.ylp.notes.domain.NoteErrorReason;
import dev.ylp.notes.domain.NoteId;
import dev.ylp.notes.domain.OwnerId;
import dev.ylp.notes.domain.Page;
import dev.ylp.notes.domain.PageRequest;
import dev.ylp.notes.domain.Title;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(NotesController.class)
@Import({SecurityConfiguration.class, CurrentOwner.class})
class NotesControllerTest {

  private static final OwnerId ADA = new OwnerId("ada");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private JwtDecoder jwtDecoder;
  @MockitoBean private CreateNoteUseCase createNoteUseCase;
  @MockitoBean private ListNotesUseCase listNotesUseCase;
  @MockitoBean private GetNoteUseCase getNoteUseCase;
  @MockitoBean private UpdateNoteUseCase updateNoteUseCase;
  @MockitoBean private DeleteNoteUseCase deleteNoteUseCase;

  private static MockHttpServletRequestBuilder asAda(MockHttpServletRequestBuilder request) {
    return request.with(jwt().jwt(token -> token.subject("ada")));
  }

  private static String body(String title, String content) {
    return """
        {"title": "%s", "content": "%s"}
        """
        .formatted(title, content);
  }

  @Test
  @DisplayName("A request without a token is rejected with 401")
  void requestWithoutTokenIsRejectedWith401() throws Exception {
    // When / Then
    mockMvc.perform(get("/api/v1/notes")).andExpect(status().isUnauthorized());
    verify(listNotesUseCase, never()).execute(any());
  }

  @Test
  @DisplayName("Creating a note returns 201, its location and the note, owned by the token subject")
  void creatingNoteReturns201ItsLocationAndTheNoteOwnedByTokenSubject() throws Exception {
    // Given
    Note note = aNote().withOwner(ADA).withTitle("Ideas").build();
    var command = new CreateNoteCommand(ADA, new Title("Ideas"), new Content("Text"));
    when(createNoteUseCase.execute(command)).thenReturn(note);

    // When / Then
    mockMvc
        .perform(
            asAda(post("/api/v1/notes"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("Ideas", "Text")))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/notes/" + note.id().value()))
        .andExpect(jsonPath("$.id").value(note.id().value().toString()))
        .andExpect(jsonPath("$.title").value("Ideas"));
  }

  @Test
  @DisplayName("Listing returns the requested page of the token subject's notes")
  void listingReturnsRequestedPageOfTokenSubjectsNotes() throws Exception {
    // Given
    Note note = aNote().withOwner(ADA).build();
    PageRequest request = new PageRequest(2, 5);
    when(listNotesUseCase.execute(new ListNotesCommand(ADA, request)))
        .thenReturn(new Page<>(List.of(note), request, 11));

    // When / Then
    mockMvc
        .perform(asAda(get("/api/v1/notes").param("page", "2").param("size", "5")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(note.id().value().toString()))
        .andExpect(jsonPath("$.page").value(2))
        .andExpect(jsonPath("$.totalItems").value(11))
        .andExpect(jsonPath("$.hasNext").value(false));
  }

  @Test
  @DisplayName("Listing without paging parameters reads the first page of 20")
  void listingWithoutPagingParametersReadsFirstPageOf20() throws Exception {
    // Given
    PageRequest request = new PageRequest(0, 20);
    when(listNotesUseCase.execute(new ListNotesCommand(ADA, request)))
        .thenReturn(new Page<>(List.of(), request, 0));

    // When / Then
    mockMvc.perform(asAda(get("/api/v1/notes"))).andExpect(status().isOk());
  }

  @Test
  @DisplayName("A page size over 100 is rejected with 400")
  void pageSizeOver100IsRejectedWith400() throws Exception {
    // When / Then
    mockMvc
        .perform(asAda(get("/api/v1/notes").param("size", "101")))
        .andExpect(status().isBadRequest())
        .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
        .andExpect(jsonPath("$.detail").value("size must be less than or equal to 100"));
  }

  @Test
  @DisplayName("Reading a note that is missing or not owned returns a 404 problem")
  void readingNoteMissingOrNotOwnedReturnsA404Problem() throws Exception {
    // Given
    NoteId id = new NoteId(UUID.randomUUID());
    when(getNoteUseCase.execute(new GetNoteCommand(ADA, id)))
        .thenThrow(NoteErrorReason.noteNotFound(id));

    // When / Then
    mockMvc
        .perform(asAda(get("/api/v1/notes/{noteId}", id.value())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("NOTE_NOT_FOUND"))
        .andExpect(jsonPath("$.type").value("urn:problem:note_not_found"))
        .andExpect(jsonPath("$.instance").value("/api/v1/notes/" + id.value()));
  }

  @Test
  @DisplayName("A title over 200 characters is rejected with 400 before reaching the use case")
  void titleOver200CharactersIsRejectedWith400BeforeReachingUseCase() throws Exception {
    // When / Then
    mockMvc
        .perform(
            asAda(post("/api/v1/notes"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("x".repeat(201), "")))
        .andExpect(status().isBadRequest());
    verify(createNoteUseCase, never()).execute(any());
  }

  @Test
  @DisplayName("A blank title passes the contract but is rejected by the domain with 400")
  void blankTitlePassesContractButIsRejectedByDomainWith400() throws Exception {
    // When / Then
    mockMvc
        .perform(
            asAda(post("/api/v1/notes"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("   ", "")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("CONSTRAINT_VIOLATION"));
  }

  @Test
  @DisplayName("A body over 64 KiB is rejected with 413")
  void bodyOver64KibIsRejectedWith413() throws Exception {
    // When / Then
    mockMvc
        .perform(
            asAda(post("/api/v1/notes"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("Big", "x".repeat(70_000))))
        .andExpect(status().isContentTooLarge());
  }

  @Test
  @DisplayName("Updating a note returns the updated note")
  void updatingNoteReturnsTheUpdatedNote() throws Exception {
    // Given
    Note note = aNote().withOwner(ADA).withTitle("New").build();
    var command = new UpdateNoteCommand(ADA, note.id(), new Title("New"), new Content("Body"));
    when(updateNoteUseCase.execute(command)).thenReturn(note);

    // When / Then
    mockMvc
        .perform(
            asAda(put("/api/v1/notes/{noteId}", note.id().value()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("New", "Body")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("New"));
  }

  @Test
  @DisplayName("Deleting a note returns 204")
  void deletingNoteReturns204() throws Exception {
    // Given
    NoteId id = new NoteId(UUID.randomUUID());

    // When / Then
    mockMvc
        .perform(asAda(delete("/api/v1/notes/{noteId}", id.value())))
        .andExpect(status().isNoContent());
    verify(deleteNoteUseCase).execute(new DeleteNoteCommand(ADA, id));
  }
}
