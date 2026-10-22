package dev.ylp.notes.adapter.out.postgres;

import dev.ylp.notes.domain.Note;
import dev.ylp.notes.domain.NoteId;
import dev.ylp.notes.domain.NoteRepository;
import dev.ylp.notes.domain.OwnerId;
import dev.ylp.notes.domain.Page;
import dev.ylp.notes.domain.PageRequest;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

/** PostgreSQL adapter of {@link NoteRepository}, the only class aware of Spring Data. */
@Repository
public class PostgresNoteRepository implements NoteRepository {

  // The id breaks ties between notes updated at the same instant, so pages never overlap.
  private static final Sort MOST_RECENTLY_UPDATED_FIRST =
      Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.asc("id"));

  private final SpringDataNoteRepository springDataNoteRepository;

  PostgresNoteRepository(SpringDataNoteRepository springDataNoteRepository) {
    this.springDataNoteRepository = springDataNoteRepository;
  }

  @Override
  public Optional<Note> findByIdAndOwner(NoteId id, OwnerId owner) {
    return springDataNoteRepository
        .findByIdAndOwnerId(id.value(), owner.value())
        .map(NoteJpaEntity::toDomain);
  }

  @Override
  public Page<Note> findByOwner(OwnerId owner, PageRequest request) {
    var page =
        springDataNoteRepository.findByOwnerId(
            owner.value(),
            org.springframework.data.domain.PageRequest.of(
                request.page(), request.size(), MOST_RECENTLY_UPDATED_FIRST));
    return new Page<>(
        page.getContent().stream().map(NoteJpaEntity::toDomain).toList(),
        request,
        page.getTotalElements());
  }

  @Override
  public void save(Note note) {
    springDataNoteRepository.save(NoteJpaEntity.fromDomain(note));
  }

  @Override
  public void delete(Note note) {
    springDataNoteRepository.deleteById(note.id().value());
  }
}
