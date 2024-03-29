package cu.sld.ucmgt.directory.service;

import cu.sld.ucmgt.directory.domain.Employee_;
import cu.sld.ucmgt.directory.domain.Phone;
import cu.sld.ucmgt.directory.domain.Phone_;
import cu.sld.ucmgt.directory.domain.WorkPlace_;
import cu.sld.ucmgt.directory.domain.elasticsearch.PhoneIndex;
import cu.sld.ucmgt.directory.repository.EmployeeRepository;
import cu.sld.ucmgt.directory.repository.PhoneRepository;
import cu.sld.ucmgt.directory.repository.WorkPlaceRepository;
import cu.sld.ucmgt.directory.repository.search.PhoneSearchRepository;
import cu.sld.ucmgt.directory.service.EmployeeService.RemovedEmployeeIndexEvent;
import cu.sld.ucmgt.directory.service.EmployeeService.SavedEmployeeIndexEvent;
import cu.sld.ucmgt.directory.service.WorkPlaceService.RemovedWorkPlaceIndexEvent;
import cu.sld.ucmgt.directory.service.WorkPlaceService.SavedWorkPlaceIndexEvent;
import cu.sld.ucmgt.directory.service.criteria.PhoneCriteria;
import cu.sld.ucmgt.directory.service.dto.PhoneDTO;
import cu.sld.ucmgt.directory.service.mapper.EmployeeMapper;
import cu.sld.ucmgt.directory.service.mapper.PhoneIndexMapper;
import cu.sld.ucmgt.directory.service.mapper.PhoneMapper;
import cu.sld.ucmgt.directory.service.mapper.WorkPlaceMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.JoinType;
import java.io.IOException;
import java.util.*;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PhoneService extends QueryService<Phone> {

    private final PhoneMapper mapper;
    private final PhoneRepository repository;
    private final EmployeeMapper employeeMapper;
    private final WorkPlaceMapper workPlaceMapper;
    private final PhoneIndexMapper phoneIndexMapper;
    private final RestHighLevelClient highLevelClient;
    private static final String INDEX_NAME = "phones";
    private final EmployeeRepository employeeRepository;
    private final PhoneSearchRepository searchRepository;
    private final WorkPlaceRepository workPlaceRepository;
    private final ApplicationEventPublisher eventPublisher;


    /**
     * Save a phone.
     *
     * @param phoneDTO the entity to save.
     * @return the persisted entity.
     */
    public PhoneDTO save(PhoneDTO phoneDTO) {
        log.debug("Request to save Phone : {}", phoneDTO);
        Phone phone = mapper.toEntity(phoneDTO);
        if (phoneDTO.getId() == null) {
            // always create enabled phone
            phoneDTO.setActive(true);
        }
        // find workplace to save in elasticsearch
        if (phone.getWorkPlace() != null) {
            workPlaceRepository.findById(phone.getWorkPlace().getId()).ifPresent(phone::setWorkPlace);
        } else {
            employeeRepository.findById(phone.getEmployee().getId()).ifPresent(phone::setEmployee);
        }
        repository.save(phone);
        PhoneIndex phoneIndex = phoneIndexMapper.toIndex(phone);
        searchRepository.save(phoneIndex);
        Map<String, Object> phoneIndexMap = createPhoneIndexToPhoneIndexMap(phoneIndex);
        final SavedPhoneIndexEvent indexEvent = SavedPhoneIndexEvent.builder()
                .phoneIndexMap(phoneIndexMap)
                .phoneId(phoneDTO.getId() == null ? null : phoneIndex.getId())
                .workPlaceId(phoneIndex.getWorkPlace() != null ? phoneIndex.getWorkPlace().getId(): null)
                .build();
        eventPublisher.publishEvent(indexEvent);
        return mapper.toDto(phone);
    }

    /**
     * Create a map of {@link PhoneIndex} instance
     *
     * @param phoneIndex {@link PhoneIndex} instance
     * @return phoneMap
     */
    private Map<String, Object> createPhoneIndexToPhoneIndexMap(PhoneIndex phoneIndex) {
        Map<String, Object> phoneMap = new HashMap<>();
        phoneMap.put("number", phoneIndex.getNumber());
        phoneMap.put("id", phoneIndex.getId().toString());
        phoneMap.put("description", phoneIndex.getDescription());
        if (phoneIndex.getWorkPlace() != null) {
            Map<String, Object> workPlaceMap = new HashMap<>();
            workPlaceMap.put("name", phoneIndex.getWorkPlace().getName());
            workPlaceMap.put("email", phoneIndex.getWorkPlace().getEmail());
            workPlaceMap.put("id", phoneIndex.getWorkPlace().getId().toString());
            workPlaceMap.put("description", phoneIndex.getWorkPlace().getDescription());
            phoneMap.put("workPlace", workPlaceMap);
        }
        return phoneMap;
    }

    @EventListener(condition = "#employeeIndexEvent.getEmployeeId() != null")
    public void updateEmployeeInPhoneIndex(SavedEmployeeIndexEvent employeeIndexEvent) {
        log.debug("Listening SavedEmployeeIndexEvent event to save EmployeeIndex with ID: {} in PhoneIndex",
                employeeIndexEvent.getEmployeeId());
            try {
                String updateCode = "for (entry in params.entrySet()){ if (entry.getKey() != \"ctx\") " +
                        "{ctx._source.employee[entry.getKey()] = entry.getValue()}}";
                UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest(INDEX_NAME)
                        .setRefresh(true)
                        .setAbortOnVersionConflict(true)
                        .setScript(new Script(ScriptType.INLINE, "painless", updateCode, employeeIndexEvent.getParams()))
                        .setQuery(QueryBuilders.matchQuery("employee.id", employeeIndexEvent.getEmployeeId()));
                highLevelClient.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT);
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    @EventListener
    public void deleteWorkPlaceInPhoneIndex(RemovedWorkPlaceIndexEvent workPlaceIndexEvent) {
        log.debug("Listening RemovedWorkPlaceIndexEvent event to PhoneIndex with WorkPlaceIndex ID: {}",
                workPlaceIndexEvent.getRemovedWorkPlaceIndexId());
        try {
            DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest(INDEX_NAME)
                    .setRefresh(true)
                    .setAbortOnVersionConflict(true)
                    .setQuery(QueryBuilders.matchQuery("workPlace.id",
                            workPlaceIndexEvent.getRemovedWorkPlaceIndexId().toString()));
            highLevelClient.deleteByQuery(deleteByQueryRequest, RequestOptions.DEFAULT);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Listen {@link SavedWorkPlaceIndexEvent} event to update workplace inside {@link PhoneIndex} index
     *
     * @param workPlaceIndexEvent information about event
     */
    @EventListener(condition = "#workPlaceIndexEvent.getWorkplaceId() != null && !#workPlaceIndexEvent.getPhoneIds().isEmpty()")
    public void updateWorkPlaceInPhoneIndex(SavedWorkPlaceIndexEvent workPlaceIndexEvent) {
        log.debug("Listening SavedWorkPlaceIndexEvent event to update WorkPlace in PhoneIndex with WorkPlaceIndex ID: {}",
                workPlaceIndexEvent.getWorkplaceId());
        try {
            String updateCode = "for (entry in params.entrySet()){if (entry.getKey() != \"ctx\") " +
                    "{ctx._source.workPlace[entry.getKey()] = entry.getValue()}}";
            UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest(INDEX_NAME)
                    .setRefresh(true)
                    .setAbortOnVersionConflict(true)
                    .setQuery(QueryBuilders.matchQuery("workPlace.id", workPlaceIndexEvent.getWorkplaceId().toString()))
                    .setScript(new Script(ScriptType.INLINE, "painless", updateCode, workPlaceIndexEvent.getWorkplaceIndexMap()));
            highLevelClient.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Listen {@link SavedWorkPlaceIndexEvent} event to update workplace inside {@link PhoneIndex} index
     *
     * @param workPlaceIndexEvent information about event
     */
    @EventListener(condition = "#workPlaceIndexEvent.getWorkplaceId() == null && !#workPlaceIndexEvent.getPhoneIds().isEmpty()")
    public void createWorkPlaceInPhoneIndex(SavedWorkPlaceIndexEvent workPlaceIndexEvent) {
        log.debug("Listening SavedWorkPlaceIndexEvent event to create WorkPlace in PhoneIndex with WorkPlaceIndex ID: {}",
                workPlaceIndexEvent.getWorkplaceId());
        workPlaceIndexEvent.getPhoneIds().forEach(phoneId -> {
            Phone phone = repository.findById(phoneId)
                 .orElseThrow(() -> new NoSuchElementException("PhoneIndex with ID: " + phoneId + " not was found"));
            PhoneIndex phoneIndex = phoneIndexMapper.toIndex(phone);
            searchRepository.save(phoneIndex);
        });
    }

    @EventListener(condition = "#event.getPhoneIds() != null")
    public void removeEmployeeIndexIntoPhoneIndex(RemovedEmployeeIndexEvent event) {
        log.debug("Listening RemovedEmployeeIndexEvent event to remove Employee in PhoneIndex with EmployeeIndex ID: {}",
                event.getRemovedEmployeeId());
        try {
            if (!event.getPhoneIds().isEmpty()) {
                BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
                event.getPhoneIds().forEach(phoneId -> boolQueryBuilder
                        .should(QueryBuilders.matchQuery("id", phoneId.toString())));

                DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest(INDEX_NAME)
                        .setRefresh(true)
                        .setAbortOnVersionConflict(true)
                        .setQuery(boolQueryBuilder);
                highLevelClient.deleteByQuery(deleteByQueryRequest, RequestOptions.DEFAULT);
            }
        } catch (IOException exception) {
            log.error(exception.getMessage());
        }
    }

    /**
     * Get all the phones.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<PhoneDTO> getAllPhones(Pageable pageable) {
        log.debug("Request to get all Phones");
        return repository.findAll(pageable).map(mapper::toDto);
    }

    /**
     * Get one phone by uid.
     *
     * @param uid the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<PhoneDTO> getPhone(UUID uid) {
        return repository.findById(uid).map(phone -> {
            PhoneDTO phoneDTO = mapper.toDto(phone);
            phoneDTO.setEmployee(employeeMapper.toDto(phone.getEmployee()));
            phoneDTO.setWorkPlace(workPlaceMapper.toDto(phone.getWorkPlace()));
            return phoneDTO;
        });
    }

    /**
     * Delete the phone by number.
     *
     * @param number the id of the entity.
     */
    public void deletePhone(String number) {
        log.debug("Request to delete Phone : {}", number);
        repository.findPhoneByNumber(number).ifPresent(phone -> {
            repository.delete(phone);
            PhoneIndex phoneIndex = searchRepository.findPhoneIndexByNumber(number)
                    .orElseThrow(() -> new NoSuchElementException("PhoneIndex with number: " + number + " not was found"));
            searchRepository.delete(phoneIndex);
            if (phoneIndex.getWorkPlace() != null) {
                final RemovedPhoneIndexEvent removedPhoneIndexEvent = RemovedPhoneIndexEvent.builder()
                        .removedPhoneIndex(phoneIndex)
                        .removedPhoneIndexId(phoneIndex.getId())
                        .workPlaceId(phoneIndex.getWorkPlace().getId())
                        .build();
                eventPublisher.publishEvent(removedPhoneIndexEvent);
            }
        });
    }

    /**
     * Delete the phone by ID.
     *
     * @param id of the entity.
     */
    public void deletePhoneById(UUID id) {
        repository.findById(id).ifPresent(phone -> {
            repository.delete(phone);
            PhoneIndex phoneIndex = searchRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("PhoneIndex with Id: " + id + " not was found"));
            searchRepository.delete(phoneIndex);
            if (phoneIndex.getWorkPlace() != null) {
                final RemovedPhoneIndexEvent removedPhoneIndexEvent = RemovedPhoneIndexEvent.builder()
                        .removedPhoneIndex(phoneIndex)
                        .removedPhoneIndexId(phoneIndex.getId())
                        .workPlaceId(phoneIndex.getWorkPlace().getId())
                        .build();
                eventPublisher.publishEvent(removedPhoneIndexEvent);
            }
        });
    }

    /**
     * Change status for active or disable phone
     *
     * @param id     phone identifier
     * @param status true or false
     * @return true if changed status or false otherwise
     */
    public Boolean changeStatus(UUID id, Boolean status) {
        log.debug("Request to change phone status to {} by ID {}", status, id);
        Optional<Phone> phoneToUpdate = repository.findById(id);
        if (phoneToUpdate.isPresent()) {
            phoneToUpdate.get().setActive(status);
            repository.save(phoneToUpdate.get());
            if (status) {
                // PhoneIndex must to be created because when Phone was disabled, PhoneIndex was removed
                PhoneIndex phoneIndexToUpdate = phoneIndexMapper.toIndex(phoneToUpdate.get());
                searchRepository.save(phoneIndexToUpdate);
                Map<String, Object> phoneIndexMap = createPhoneIndexToPhoneIndexMap(phoneIndexToUpdate);
                final SavedPhoneIndexEvent savedPhoneIndexEvent = SavedPhoneIndexEvent.builder()
                        .phoneId(null)
                        .phoneIndexMap(phoneIndexMap)
                        .workPlaceId(phoneIndexToUpdate.getWorkPlace() != null ? phoneIndexToUpdate.getWorkPlace().getId() : null)
                        .build();
                eventPublisher.publishEvent(savedPhoneIndexEvent);
            } else {
                // PhoneIndex must to be removed because Phone was disabled
                searchRepository.findPhoneIndexByNumber(phoneToUpdate.get().getNumber()).ifPresent(phoneIndexToDisable -> {
                    searchRepository.delete(phoneIndexToDisable);
                    final RemovedPhoneIndexEvent removedPhoneIndexEvent = RemovedPhoneIndexEvent.builder()
                            .removedPhoneIndex(phoneIndexToDisable)
                            .removedPhoneIndexId(phoneIndexToDisable.getId())
                            .workPlaceId(phoneIndexToDisable.getWorkPlace() != null ?
                                    phoneIndexToDisable.getWorkPlace().getId() : null)
                            .build();
                    eventPublisher.publishEvent(removedPhoneIndexEvent);
                });
            }
            return true;
        }
        return false;
    }

    /**
     * Return a {@link List} of {@link PhoneDTO} which matches the criteria from the database.
     *
     * @param join Logical operator to join expression: AND - OR
     * @param criteria       The object which holds all the filters, which the entities should match.
     * @return the matching entities.
     */
    public Page<PhoneDTO> findByCriteria(String join, PhoneCriteria criteria, Pageable pageable) {
        final Specification<Phone> specification = createSpecification(join, criteria);
        return repository.findAll(specification, pageable).map(mapper::toDto);
    }

    /**
     * Function to convert {@link PhoneCriteria} to a {@link Specification}
     * @param join Logical operator to join expression: AND - OR
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    private Specification<Phone> createSpecification(String join, PhoneCriteria criteria) {
        Specification<Phone> specification = Specification.where(null);
        if (criteria != null) {
            if (join.equalsIgnoreCase("AND")) {
                if (criteria.getId() != null) {
                    specification = specification.and(buildSpecification(criteria.getId(), Phone_.id));
                }
                if (criteria.getActive() != null) {
                    specification = specification.and(buildSpecification(criteria.getActive(), Phone_.active));
                }
                if (criteria.getNumber() != null) {
                    specification = specification.and(buildStringSpecification(criteria.getNumber(), Phone_.number));
                }
                if (criteria.getDescription() != null) {
                    specification = specification.and(buildStringSpecification(criteria.getDescription(), Phone_.description));
                }
                if (criteria.getEmployeeName() != null) {
                    specification = specification.and(buildSpecification(criteria.getEmployeeName(),
                            root -> root.join(Phone_.employee, JoinType.LEFT).get(Employee_.name)));
                }
                if (criteria.getWorkPlaceName() != null) {
                    specification = specification.and(buildSpecification(criteria.getWorkPlaceName(),
                            root -> root.join(Phone_.workPlace, JoinType.LEFT).get(WorkPlace_.name)));
                }
            } else {
                if (criteria.getId() != null) {
                    specification = specification.or(buildSpecification(criteria.getId(), Phone_.id));
                }
                if (criteria.getActive() != null) {
                    specification = specification.or(buildSpecification(criteria.getActive(), Phone_.active));
                }
                if (criteria.getNumber() != null) {
                    specification = specification.or(buildStringSpecification(criteria.getNumber(), Phone_.number));
                }
                if (criteria.getDescription() != null) {
                    specification = specification.or(buildStringSpecification(criteria.getDescription(), Phone_.description));
                }
                if (criteria.getEmployeeName() != null) {
                    specification = specification.or(buildSpecification(criteria.getEmployeeName(),
                            root -> root.join(Phone_.employee, JoinType.LEFT).get(Employee_.name)));
                }
                if (criteria.getWorkPlaceName() != null) {
                    specification = specification.or(buildSpecification(criteria.getWorkPlaceName(),
                            root -> root.join(Phone_.workPlace, JoinType.LEFT).get(WorkPlace_.name)));
                }
            }
        }
        return specification;
    }

    /**
     * Class to register a saved {@link PhoneIndex} as event
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class SavedPhoneIndexEvent {
        private UUID phoneId;
        private UUID workPlaceId;
        private Map<String, Object> phoneIndexMap;
    }

    /**
     * Class to register a removed {@link PhoneIndex} as event
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class RemovedPhoneIndexEvent {
        private UUID removedPhoneIndexId;
        private UUID workPlaceId;
        private PhoneIndex removedPhoneIndex;

    }

}
