package cu.sld.ucmgt.directory.service;

import cu.sld.ucmgt.directory.domain.Employee;
import cu.sld.ucmgt.directory.domain.Phone;
import cu.sld.ucmgt.directory.domain.WorkPlace;
import cu.sld.ucmgt.directory.domain.elasticsearch.WorkPlaceIndex;
import cu.sld.ucmgt.directory.event.UpdatedEmployeeIndexEvent;
import cu.sld.ucmgt.directory.repository.EmployeeRepository;
import cu.sld.ucmgt.directory.repository.PhoneRepository;
import cu.sld.ucmgt.directory.repository.WorkPlaceRepository;
import cu.sld.ucmgt.directory.repository.search.WorkPlaceSearchRepository;
import cu.sld.ucmgt.directory.service.dto.WorkPlaceDTO;
import cu.sld.ucmgt.directory.service.mapper.WorkPlaceIndexMapper;
import cu.sld.ucmgt.directory.service.mapper.WorkPlaceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
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
    private static final String INDEX_NAME = "workplaces";

    /**
     * Save a workplace.
     *
     * @param workPlaceDTO the entity to save.
     * @return the persisted entity.
     */
    public WorkPlace save(WorkPlaceDTO workPlaceDTO) {
        WorkPlace workPlace = mapper.toEntity(workPlaceDTO);
        // find all employees and phones to saves
        if (workPlaceDTO.getEmployees() != null) {
            Set<Employee> employees = workPlaceDTO.getEmployees().stream()
                    .map(employeeRepository::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
            workPlace.setEmployees(employees);
        }
        if (workPlaceDTO.getPhones() != null) {
            Set<Phone> phones = workPlaceDTO.getPhones().stream()
                    .map(phoneRepository::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
            workPlace.setPhones(phones);
        }
        repository.save(workPlace);
        return workPlace;
    }

    /**
     * Create a workplace.
     *
     * @param workPlaceDTO the entity to save.
     * @return the persisted entity.
     */
    public WorkPlaceDTO create(WorkPlaceDTO workPlaceDTO) {
        log.debug("Request to create WorkPlace : {}", workPlaceDTO);
        WorkPlace workPlace = save(workPlaceDTO);
        WorkPlaceIndex workPlaceIndex = workPlaceIndexMapper.toIndex(workPlace);
        searchRepository.save(workPlaceIndex);
        return mapper.toDto(workPlace);
    }

    /**
     * Update a workplace.
     *
     * @param workPlaceDTO the entity to save.
     * @return the persisted entity.
     */
    public WorkPlaceDTO update(WorkPlaceDTO workPlaceDTO) {
        log.debug("Request to update WorkPlace : {}", workPlaceDTO);
        WorkPlace workPlace = save(workPlaceDTO);
        try {
            // updating the workplace belonging to phones and employees
            Map<String, Object> params = new HashMap<>();
            params.put("name", workPlaceDTO.getName());
            params.put("email", workPlaceDTO.getEmail());
            params.put("description", workPlaceDTO.getDescription());
            String updateCode = "for (entry in params.entrySet()){if (entry.getKey() != \"ctx\") " +
                    "{ctx._source.workPlace[entry.getKey()] = entry.getValue()}}";
            UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest("phones", "employees")
                    .setRefresh(true)
                    .setAbortOnVersionConflict(true)
                    .setQuery(QueryBuilders.matchQuery("workPlace.id", workPlace.getId().toString()))
                    .setScript(new Script(ScriptType.INLINE, "painless", updateCode, params));
            highLevelClient.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mapper.toDto(workPlace);
    }

    @EventListener
    public void saveEmployeeIndexInWorkPlaceIndex(UpdatedEmployeeIndexEvent employeeIndexEvent) {
        try {
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
                    .setScript(new Script(ScriptType.INLINE, "painless", updateCode, employeeIndexEvent.getParams()));
            highLevelClient.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventListener
    public void savePhoneInWorkPlaceIndex(PhoneService.SavedPhoneIndexEvent phoneIndexEvent) {
        log.debug("Listening SavedPhoneIndexEvent event with PhoneIndex ID: {}", phoneIndexEvent.getPhoneId());
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
             .setScript(new Script(ScriptType.INLINE, "painless", updateCode, phoneIndexEvent.getPhoneIndexMap()));
            highLevelClient.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT);
        } catch (ElasticsearchException | IOException e) {
            e.printStackTrace();
        }
    }

    @EventListener
    public void removePhoneIndexInWorkPlaceIndex(PhoneService.RemovedPhoneIndexEvent event) {
        log.debug("Listening RemovedPhoneIndexEvent event with PhoneIndex ID: {}", event.getRemovedPhoneIndexId());
        try {
            String updateCode = "ctx._source.phones.removeIf(phone -> phone.id == \"" + event.getRemovedPhoneIndexId().toString() + "\")";
            UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest("workplaces")
                    .setRefresh(true)
                    .setAbortOnVersionConflict(true)
                    .setScript(new Script(ScriptType.INLINE, "painless", updateCode, Collections.emptyMap()));
            highLevelClient.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT);
        } catch (ElasticsearchException | IOException exception) {
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
            this.deleteWorkPlaceInPhoneIndex(workPlace.getId().toString());
            this.updateWorkPlaceInEmployeeIndex(workPlace.getId().toString());
        });

    }

    private void updateWorkPlaceInEmployeeIndex(String id) {
        try {
            UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest("employees")
                    .setRefresh(true)
                    .setAbortOnVersionConflict(true)
                    .setQuery(QueryBuilders.matchQuery("workPlace.id", id))
                    .setScript(new Script(ScriptType.INLINE, "painless",
                            "ctx._source.workPlace=null", Collections.emptyMap()));
            highLevelClient.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private void deleteWorkPlaceInPhoneIndex(String id) {
        try {
            DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest("phones")
                    .setRefresh(true)
                    .setAbortOnVersionConflict(true)
                    .setQuery(QueryBuilders.matchQuery("workPlace.id", id));
            highLevelClient.deleteByQuery(deleteByQueryRequest, RequestOptions.DEFAULT);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
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
}
