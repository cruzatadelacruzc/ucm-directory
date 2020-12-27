package cu.sld.ucmgt.directory.service;

import cu.sld.ucmgt.directory.config.AppProperties;
import cu.sld.ucmgt.directory.config.audit.AuditEventConverter;
import cu.sld.ucmgt.directory.repository.PersistenceAuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing audit events.
 * <p>
 * This is the default implementation to support SpringBoot Actuator {@code AuditEventRepository}.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuditEventService {

    private final AppProperties properties;
    private final AuditEventConverter auditEventConverter;
    private final PersistenceAuditEventRepository persistenceAuditEventRepository;

    /**
     * Old audit events should be automatically deleted after 30 days.
     *
     * This is scheduled to get fired at 12:00 (am).
     */
    @Scheduled(cron = "0 0 12 * * ?")
    public void removeOldAuditEvents() {
        persistenceAuditEventRepository.findByAuditEventDateBefore(Instant.now().minus(properties.getAuditEvents().getRetentionPeriod(), ChronoUnit.DAYS))
                .forEach(persistentAuditEvent -> {
                    log.debug("Deleting audit data {}", persistentAuditEvent);
                    persistenceAuditEventRepository.delete(persistentAuditEvent);
                });
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> findAll(Pageable pageable){
        return persistenceAuditEventRepository.findAll(pageable)
                .map(auditEventConverter::convertToAuditEvent);
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> findByDates(Instant from, Instant to, Pageable pageable) {
        return persistenceAuditEventRepository.findAllByAuditEventDateBetween(from, to, pageable)
                .map(auditEventConverter::convertToAuditEvent);
    }

    @Transactional(readOnly = true)
    public Optional<AuditEvent> findById(UUID id) {
        return persistenceAuditEventRepository.findById(id)
                .map(auditEventConverter::convertToAuditEvent);
    }
}
