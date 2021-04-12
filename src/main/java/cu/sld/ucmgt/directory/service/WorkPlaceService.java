package cu.sld.ucmgt.directory.service;

import cu.sld.ucmgt.directory.domain.Employee;
import cu.sld.ucmgt.directory.domain.Phone;
import cu.sld.ucmgt.directory.domain.WorkPlace;
import cu.sld.ucmgt.directory.domain.elasticsearch.EmployeeIndex;
import cu.sld.ucmgt.directory.domain.elasticsearch.PhoneIndex;
import cu.sld.ucmgt.directory.domain.elasticsearch.WorkPlaceIndex;
import cu.sld.ucmgt.directory.repository.EmployeeRepository;
import cu.sld.ucmgt.directory.repository.PhoneRepository;
import cu.sld.ucmgt.directory.repository.WorkPlaceRepository;
import cu.sld.ucmgt.directory.repository.search.WorkPlaceSearchRepository;
import cu.sld.ucmgt.directory.service.EmployeeService.RemovedEmployeeIndexEvent;
import cu.sld.ucmgt.directory.service.EmployeeService.SavedEmployeeIndexEvent;
import cu.sld.ucmgt.directory.service.PhoneService.RemovedPhoneIndexEvent;
import cu.sld.ucmgt.directory.service.PhoneService.SavedPhoneIndexEvent;
import cu.sld.ucmgt.directory.service.dto.WorkPlaceDTO;
import cu.sld.ucmgt.directory.service.mapper.WorkPlaceIndexMapper;
import cu.sld.ucmgt.directory.service.mapper.WorkPlaceMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class WorkPlaceService {

    private final WorkPlaceMapper mapper;
    private final WorkPlaceRepository repository;
    private final PhoneRepository phoneRepository;
    private final RestHighLevelClient highLevelClient;
    private final EmployeeRepository employeeRepository;
    private final WorkPlaceIndexMapper workPlaceIndexMapper;
    private final WorkPlaceSearchRepository searchRepository;
    private final ApplicationEventPublisher eventPublisher;
    private static final String INDEX_NAME = "workplaces";

    /**
     * Save a workplace.
     *
     * @param workPlaceDTO the entity to save.
     * @return the persisted entity.
     */
    public WorkPlaceDTO save(WorkPlaceDTO workPlaceDTO) {
        log.debug("Request to save WorkPlace : {}", workPlaceDTO);
        WorkPlace workPlace = mapper.toEntity(workPlaceDTO);
        // find all employees and phones to saves
        if (workPlaceDTO.getEmployees() != null) {
            Set<Employee> employees = workPlaceDTO.getEmployees().stream()
                    .map(employeeRepository::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
            employees.forEach(workPlace::addEmployee);
        }
        if (workPlaceDTO.getPhones() != null) {
            Set<Phone> phones = workPlaceDTO.getPhones().stream()
                    .map(phoneRepository::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
            phones.forEach(workPlace::addPhone);
        }
        repository.save(workPlace);
        WorkPlaceIndex workPlaceIndex = workPlaceIndexMapper.toIndex(workPlace);
        searchRepository.save(workPlaceIndex);
        // saving the workplace belonging to phones and employees
        Map<String, Object> workPlaceMap = convertWorkPlaceIndexToWorkPlaceIndexMap(workPlaceIndex);
        List<UUID> employeeIds = workPlace.getEmployees().stream().map(Employee::getId).collect(Collectors.toList());
        List<UUID> phoneIds = workPlace.getPhones().stream().map(Phone::getId).collect(Collectors.toList());
        final SavedWorkPlaceIndexEvent savedWorkPlaceIndexEvent = SavedWorkPlaceIndexEvent.builder()
                .workplaceId( workPlaceDTO.getId() != null ? workPlaceIndex.getId(): null)
                .phoneIds(phoneIds)
                .employeeIds(employeeIds)
                .workplaceIndexMap(workPlaceMap)
                .build();
        eventPublisher.publishEvent(savedWorkPlaceIndexEvent);
        return mapper.toDto(workPlace);
    }

    /**
     * Create a Map of {@link WorkPlaceIndex} instance
     *
     * @param workPlaceIndex {@link WorkPlaceIndex} instance
     * @return employeeIndexMap
     */
    private Map<String, Object> convertWorkPlaceIndexToWorkPlaceIndexMap(WorkPlaceIndex workPlaceIndex) {
        Map<String, Object> workPlaceIndexMap = new HashMap<>();
        workPlaceIndexMap.put("id", workPlaceIndex.getId().toString());
        workPlaceIndexMap.put("name", workPlaceIndex.getName());
        workPlaceIndexMap.put("email", workPlaceIndex.getEmail());
        workPlaceIndexMap.put("description", workPlaceIndex.getDescription());
        return workPlaceIndexMap;
    }

    @EventListener
    public void saveEmployeeIndexInWorkPlaceIndex(SavedEmployeeIndexEvent employeeIndexEvent) {
        log.debug("Listening SavedEmployeeIndexEvent event to save EmployeeIndex with ID: {} in WorkPlaceIndex",
                employeeIndexEvent.getEmployeeId());
        Object workPlaceMap = employeeIndexEvent.getParams().getOrDefault("workPlace", null);
        if (workPlaceMap != null) {
            try {
                String workPlaceId = (String) ((HashMap) workPlaceMap).get("id");
                // avoid redundant data, employee.workplace equals current workplace
                employeeIndexEvent.getParams().replace("workPlace", null);
                String updateCode = "params.remove(\"ctx\");ctx._source.employees.add(params)";
                if (employeeIndexEvent.getEmployeeId() != null) {
                    updateCode = "def targets = ctx._source.employees.findAll(employee " +
                            "-> employee.id == \"" + employeeIndexEvent.getEmployeeId() + "\" ); " +
                            "for (employee in targets) { for (entry in params.entrySet()) { if (entry.getKey() != \"ctx\") {" +
                            "employee[entry.getKey()] = entry.getValue() }}}";
                }
                UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest(INDEX_NAME)
                        .setRefresh(true)
                        .setAbortOnVersionConflict(true)
                        .setQuery(QueryBuilders.matchQuery("id", workPlaceId))
                        .setScript(new Script(ScriptType.INLINE, "painless", updateCode, employeeIndexEvent.getParams()));
                highLevelClient.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @EventListener(condition = "#phoneIndexEvent.getWorkPlaceId() != null")
    public void savePhoneInWorkPlaceIndex(SavedPhoneIndexEvent phoneIndexEvent) {
        log.debug("Listening SavedPhoneIndexEvent event to save Phone in WorkPlaceIndex with PhoneIndex ID: {}",
                phoneIndexEvent.getPhoneId());
        try {
            // updating the phone belonging to workplaces
            String updateCode = "params.remove(\"ctx\");ctx._source.phones.add(params)";
            if (phoneIndexEvent.getPhoneId() != null) {
                updateCode = "def targets = ctx._source.phones.findAll(phone -> " +
                        "phone.id == \"" + phoneIndexEvent.getPhoneId() + "\" ); " +
                        "for (phone in targets) { for (entry in params.entrySet()) { if (entry.getKey() != \"ctx\") {" +
                        "phone[entry.getKey()] = entry.getValue() }}}";
            }
            UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest(INDEX_NAME)
             .setRefresh(true)
             .setAbortOnVersionConflict(true)
             .setQuery(QueryBuilders.matchQuery("id", phoneIndexEvent.getWorkPlaceId().toString()))
             .setScript(new Script(ScriptType.INLINE, "painless", updateCode, phoneIndexEvent.getPhoneIndexMap()));
            highLevelClient.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT);
        } catch (ElasticsearchException | IOException e) {
            e.printStackTrace();
        }
    }

    @EventListener
    public void removePhoneIndexInWorkPlaceIndex(RemovedPhoneIndexEvent event) {
        log.debug("Listening RemovedPhoneIndexEvent event to remove Phone in WorkPlaceIndex with PhoneIndex ID: {}",
                event.getRemovedPhoneIndexId());
        try {
            String updateCode = "ctx._source.phones.removeIf(phone -> phone.id == \"" + event.getRemovedPhoneIndexId().toString() + "\")";
            UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest("workplaces")
                    .setRefresh(true)
                    .setAbortOnVersionConflict(true)
                    .setQuery(QueryBuilders.matchQuery("id", event.getWorkPlaceId().toString()))
                    .setScript(new Script(ScriptType.INLINE, "painless", updateCode, Collections.emptyMap()));
            highLevelClient.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT);
        } catch (ElasticsearchException | IOException exception) {
            exception.printStackTrace();
        }
    }

    @EventListener
    public void removeEmployeeIndexInWorkPlaceIndex(RemovedEmployeeIndexEvent event) {
        log.debug("Listening RemovedEmployeeIndexEvent event to remove Employee in WorkPlaceIndex with EmployeeIndex ID: {}"
                , event.getRemovedEmployeeId());
        try {
            String updateCode = "ctx._source.employees.removeIf(employee -> employee.id == \"" + event
                    .getRemovedEmployeeId().toString() + "\")";
            UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest(INDEX_NAME)
                    .setRefresh(true)
                    .setAbortOnVersionConflict(true)
                    .setScript(new Script(ScriptType.INLINE, "painless", updateCode, Collections.emptyMap()));
            highLevelClient.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT);
        }catch (IOException exception){
            exception.printStackTrace();
        }
    }

    /**
     * Delete the workplace by id.
     *
     * @param uid the id of the entity.
     */
    public void deleteWorkPlace(UUID uid) {
        log.debug("Request to delete WorkPlace : {}", uid);
        repository.findWorkPlaceWithAssociationsById(uid).ifPresent(workPlace -> {
            new HashSet<>(workPlace.getEmployees()).forEach(workPlace::removeEmployee);
            repository.delete(workPlace);
            searchRepository.deleteById(workPlace.getId());
            WorkPlaceIndex workPlaceIndex = workPlaceIndexMapper.toIndex(workPlace);
            final RemovedWorkPlaceIndexEvent removedWorkPlaceIndexEvent = RemovedWorkPlaceIndexEvent.builder()
                    .removedWorkPlaceIndexId(workPlaceIndex.getId())
                    .removedWorkPlaceIndex(workPlaceIndex)
                    .build();
            eventPublisher.publishEvent(removedWorkPlaceIndexEvent);
        });

    }

    /**
     * Get one workplace by uid.
     *
     * @param uid the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<WorkPlaceDTO> getWorkPlace(UUID uid) {
        log.debug("Request to get WorkPlace : {}", uid);
        return repository.findById(uid).map(mapper::toDto);
    }

    /**
     * Get all the workplaces.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<WorkPlaceDTO> getAllWorkPlaces(Pageable pageable) {
        log.debug("Request to get all WorkPlaces");
        return repository.findAll(pageable).map(mapper::toDto);
    }

    /**
     * Change status for active or disable workplace
     *
     * @param id          workplace identifier
     * @param status true or false
     * @return true if changed status or false otherwise
     */
    public Boolean changeStatus(UUID id, Boolean status) {
        log.debug("Request to change WorkPlace status to {} by ID {}", status, id);
        Optional<WorkPlace> workPlaceToUpdate  = repository.findWorkPlaceWithAssociationsById(id);
        if (workPlaceToUpdate.isPresent()){
            workPlaceToUpdate.get().setActive(status);
            repository.save(workPlaceToUpdate.get());
            if (status) {
                // WorkPlaceIndex must to be created because when WorkPlace was disabled, WorkPlaceIndex was removed
                WorkPlaceIndex workPlaceIndex = workPlaceIndexMapper.toIndex(workPlaceToUpdate.get());
                searchRepository.save(workPlaceIndex);
                Map<String, Object> workPlaceIndexMap = convertWorkPlaceIndexToWorkPlaceIndexMap(workPlaceIndex);
                List<UUID> employeeIds = workPlaceIndex.getEmployees().stream().map(EmployeeIndex::getId).collect(Collectors.toList());
                List<UUID> phoneIds = workPlaceIndex.getPhones().stream().map(PhoneIndex::getId).collect(Collectors.toList());
                final SavedWorkPlaceIndexEvent savedWorkPlaceIndexEvent = SavedWorkPlaceIndexEvent.builder()
                        .employeeIds(employeeIds)
                        .phoneIds(phoneIds)
                        .workplaceIndexMap(workPlaceIndexMap)
                        .build();
                eventPublisher.publishEvent(savedWorkPlaceIndexEvent);
            } else {
                // WorkPlaceIndex must to be removed because WorkPlace was disabled
                WorkPlaceIndex workPlaceIndex = searchRepository.findById(workPlaceToUpdate.get().getId())
                        .orElseThrow(() -> new NoSuchElementException("WorkPlaceIndex with ID: "
                                + workPlaceToUpdate.get().getId() + " not was found"));
                searchRepository.delete(workPlaceIndex);
                final RemovedWorkPlaceIndexEvent removedWorkPlaceIndexEvent = RemovedWorkPlaceIndexEvent.builder()
                        .removedWorkPlaceIndexId(workPlaceIndex.getId())
                        .removedWorkPlaceIndex(workPlaceIndex)
                        .build();
                eventPublisher.publishEvent(removedWorkPlaceIndexEvent);
            }
            return true;
        };
        return false;
    }

    /**
     *  Class to register a removed {@link WorkPlaceIndex} as event
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class RemovedWorkPlaceIndexEvent {
        UUID removedWorkPlaceIndexId;
        WorkPlaceIndex removedWorkPlaceIndex;
    }

    /**
     *  Class to register a saved {@link WorkPlaceIndex} as event
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class SavedWorkPlaceIndexEvent{
        private UUID workplaceId;
        private Map<String,Object> workplaceIndexMap;
        private List<UUID> employeeIds;
        private List<UUID> phoneIds;
    }
}
