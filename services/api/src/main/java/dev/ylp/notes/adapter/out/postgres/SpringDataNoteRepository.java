package dev.ylp.notes.adapter.out.postgres;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataNoteRepository extends JpaRepository<NoteJpaEntity, UUID> {

  Optional<NoteJpaEntity> findByIdAndOwnerId(UUID id, String ownerId);

  Page<NoteJpaEntity> findByOwnerId(String ownerId, Pageable pageable);
}
