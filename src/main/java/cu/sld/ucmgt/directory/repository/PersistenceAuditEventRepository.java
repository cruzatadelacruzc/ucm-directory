package cu.sld.ucmgt.directory.repository;

import cu.sld.ucmgt.directory.domain.PersistentAuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link PersistentAuditEvent} entity.
 */
@Repository
public interface PersistenceAuditEventRepository extends JpaRepository<PersistentAuditEvent, UUID> {

    List<PersistentAuditEvent> findByAuditEventDateBefore(Instant before);

    Page<PersistentAuditEvent> findAllByAuditEventDateBetween(Instant fromDate, Instant toDate, Pageable pageable);
}
