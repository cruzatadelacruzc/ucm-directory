package cu.sld.ucmgt.directory.service;

import cu.sld.ucmgt.directory.domain.Employee;
import cu.sld.ucmgt.directory.domain.Phone;
import cu.sld.ucmgt.directory.domain.WorkPlace;
import cu.sld.ucmgt.directory.domain.elasticsearch.WorkPlaceIndex;
import cu.sld.ucmgt.directory.repository.EmployeeRepository;
import cu.sld.ucmgt.directory.repository.PhoneRepository;
import cu.sld.ucmgt.directory.repository.WorkPlaceRepository;
import cu.sld.ucmgt.directory.repository.search.WorkPlaceSearchRepository;
import cu.sld.ucmgt.directory.service.dto.WorkPlaceDTO;
import cu.sld.ucmgt.directory.service.mapper.WorkPlaceIndexMapper;
import cu.sld.ucmgt.directory.service.mapper.WorkPlaceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
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
            log.error(e.getMessage());
        }
        return mapper.toDto(workPlace);
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
            searchRepository.deleteById(uid);
            try {
                DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest("phones")
                        .setRefresh(true)
                        .setAbortOnVersionConflict(true)
                        .setQuery(QueryBuilders.matchQuery("workPlace.id", uid.toString()));

                UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest("employees")
                        .setRefresh(true)
                        .setAbortOnVersionConflict(true)
                        .setQuery(QueryBuilders.matchQuery("workPlace.id", workPlace.getId().toString()))
                        .setScript(new Script(ScriptType.INLINE, "painless",
                                "ctx._source.workPlace=null", Collections.emptyMap()));
             highLevelClient.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT);

                highLevelClient.deleteByQuery(deleteByQueryRequest, RequestOptions.DEFAULT);
            } catch (IOException exception) {
                log.error(exception.getMessage());
            }
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
}
