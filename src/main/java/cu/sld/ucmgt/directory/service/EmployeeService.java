package cu.sld.ucmgt.directory.service;

import cu.sld.ucmgt.directory.domain.Employee;
import cu.sld.ucmgt.directory.domain.NomenclatureType;
import cu.sld.ucmgt.directory.domain.Phone;
import cu.sld.ucmgt.directory.domain.elasticsearch.EmployeeIndex;
import cu.sld.ucmgt.directory.repository.EmployeeRepository;
import cu.sld.ucmgt.directory.repository.NomenclatureRepository;
import cu.sld.ucmgt.directory.repository.WorkPlaceRepository;
import cu.sld.ucmgt.directory.repository.search.EmployeeSearchRepository;
import cu.sld.ucmgt.directory.service.NomenclatureService.SavedNomenclatureEvent;
import cu.sld.ucmgt.directory.service.WorkPlaceService.RemovedWorkPlaceIndexEvent;
import cu.sld.ucmgt.directory.service.WorkPlaceService.SavedWorkPlaceIndexEvent;
import cu.sld.ucmgt.directory.service.dto.EmployeeDTO;
import cu.sld.ucmgt.directory.service.mapper.EmployeeIndexMapper;
import cu.sld.ucmgt.directory.service.mapper.EmployeeMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
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
public class EmployeeService {

    private final EmployeeMapper mapper;
    private final EmployeeRepository repository;
    private final RestHighLevelClient highLevelClient;
    private static final String INDEX_NAME = "employees";
    private final WorkPlaceRepository workPlaceRepository;
    private final EmployeeIndexMapper employeeIndexMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final EmployeeSearchRepository searchRepository;
    private final NomenclatureRepository nomenclatureRepository;

    /**
     * Save a employee.
     *
     * @param employeeDTO the entity to save.
     * @return the persisted entity.
     */
    public Employee save(EmployeeDTO employeeDTO) {
        Employee employee = mapper.toEntity(employeeDTO);
        repository.save(employee);
        // find all nomenclatures and workplace to save in elasticsearch
        if (employee.getCategory() != null) {
            nomenclatureRepository.findById(employee.getCategory().getId()).ifPresent(employee::setCategory);
        }
        if (employee.getCharge() != null) {
            nomenclatureRepository.findById(employee.getCharge().getId()).ifPresent(employee::setCharge);
        }
        if (employee.getSpecialty() != null) {
            nomenclatureRepository.findById(employee.getSpecialty().getId()).ifPresent(employee::setSpecialty);
        }
        if (employee.getDistrict() != null) {
            nomenclatureRepository.findById(employee.getDistrict().getId()).ifPresent(employee::setDistrict);
        }
        if (employee.getProfession() != null) {
            nomenclatureRepository.findById(employee.getProfession().getId()).ifPresent(employee::setProfession);
        }
        if (employee.getWorkPlace() != null) {
            workPlaceRepository.findById(employee.getWorkPlace().getId()).ifPresent(employee::setWorkPlace);
        }
        return employee;
    }

    /**
     * Create a employee.
     *
     * @param employeeDTO the entity to save.
     * @return the persisted entity.
     */
    public EmployeeDTO create(EmployeeDTO employeeDTO) {
        log.debug("Request to create Employee : {}", employeeDTO);
        Employee employee = save(employeeDTO);
        EmployeeIndex employeeIndex = employeeIndexMapper.toIndex(employee);
        searchRepository.save(employeeIndex);
        // saving the EmployeeIndex belonging to PhoneIndex and WorkPlaceIndex
        Map<String, Object> employeeIndexMap = createEmployeeToEmployeeIndexMap(employee);
        final SavedEmployeeIndexEvent savedEmployeeIndexEvent = SavedEmployeeIndexEvent.builder()
                .params(employeeIndexMap)
                .build();
        eventPublisher.publishEvent(savedEmployeeIndexEvent);
        return mapper.toDto(employee);
    }

    /**
     * Update a employee and phone's employee.
     *
     * @param employeeDTO the entity to save.
     * @return the persisted entity.
     */
    public EmployeeDTO update(EmployeeDTO employeeDTO) {
        log.debug("Request to update Employee : {}", employeeDTO);
        Employee employee = save(employeeDTO);
        searchRepository.save(employeeIndexMapper.toIndex(employee));
        // updating the EmployeeIndex belonging to PhoneIndex and WorkPlaceIndex
        Map<String, Object> employeeIndexMap = createEmployeeToEmployeeIndexMap(employee);
        final SavedEmployeeIndexEvent savedEmployeeIndexEvent = SavedEmployeeIndexEvent.builder()
                .employeeId(employee.getId().toString())
                .params(employeeIndexMap)
                .build();
        eventPublisher.publishEvent(savedEmployeeIndexEvent);
        return mapper.toDto(employee);
    }

    /**
     * Create a map of {@link Employee} instance
     *
     * @param employee {@link Employee} instance
     * @return employeeIndexMap
     */
    private Map<String, Object> createEmployeeToEmployeeIndexMap(Employee employee) {
        Map<String, Object> params = new HashMap<>();
        params.put("ci", employee.getCi());
        params.put("age", employee.getAge());
        params.put("race", employee.getRace());
        params.put("name", employee.getName());
        params.put("email", employee.getEmail());
        params.put("gender", employee.getGender());
        params.put("address", employee.getAddress());
        params.put("id", employee.getId().toString());
        params.put("birthdate", employee.getBirthdate());
        params.put("firstLastName", employee.getFirstLastName());
        params.put("registerNumber", employee.getRegisterNumber());
        params.put("secondLastName", employee.getSecondLastName());
        params.put("professionalNumber", employee.getProfessionalNumber());
        params.put("charge", employee.getCharge() != null ? employee.getCharge().getName() : null);
        params.put("district", employee.getDistrict() != null ? employee.getDistrict().getName() : null);
        params.put("category", employee.getCategory() != null ? employee.getCategory().getName() : null);
        params.put("specialty", employee.getSpecialty() != null ? employee.getSpecialty().getName() : null);
        params.put("profession", employee.getProfession() != null ? employee.getProfession().getName() : null);
        if (employee.getWorkPlace() != null) {
            Map<String, Object> workplaceMap = new HashMap<>();
            workplaceMap.put("name", employee.getWorkPlace().getName());
            workplaceMap.put("email", employee.getWorkPlace().getEmail());
            workplaceMap.put("id", employee.getWorkPlace().getId().toString());
            workplaceMap.put("description", employee.getWorkPlace().getDescription());
            params.put("workPlace", workplaceMap);
        }
        return params;
    }


    /**
     * Delete the employee by uid.
     *
     * @param uid the id of the entity.
     */
    public void deleteEmployee(UUID uid) {
        log.debug("Request to delete Employee : {}", uid);
        repository.findById(uid).ifPresent(employee -> {
           repository.delete(employee);
           EmployeeIndex employeeIndex = searchRepository.findById(employee.getId())
            .orElseThrow(() -> new NoSuchElementException("EmployeeIndex with ID: " + employee.getId() + " not was found"));
           searchRepository.delete(employeeIndex);
           List<UUID> phoneIds = employee.getPhones().stream().map(Phone::getId).collect(Collectors.toList());
           final RemovedEmployeeIndexEvent removedEmployeeIndexEvent = RemovedEmployeeIndexEvent.builder()
                   .phoneIds(phoneIds)
                   .removedEmployeeIndex(employeeIndex)
                   .removedEmployeeId(employeeIndex.getId())
                   .workPlaceId(employee.getWorkPlace() != null ? employee.getWorkPlace().getId() : null)
                   .build();
           eventPublisher.publishEvent(removedEmployeeIndexEvent);
        });
    }

    /**
     * Listen {@link SavedWorkPlaceIndexEvent} event to update workplace inside {@link EmployeeIndex} index
     * @param workPlaceIndexEvent information about event
     */
    @EventListener(condition = "#workPlaceIndexEvent.workplaceId != null && !#workPlaceIndexEvent.getEmployeeIds().isEmpty()")
    public void updateWorkPlaceIntoEmployeeIndex(SavedWorkPlaceIndexEvent workPlaceIndexEvent) {
        log.debug("Listening SavedWorkPlaceIndexEvent event to update WorkPlace in EmployeeIndex with WorkPlaceIndex ID: {}",
                workPlaceIndexEvent.getWorkplaceId());
        try {
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            workPlaceIndexEvent.getEmployeeIds().forEach(employeeId -> boolQueryBuilder
                    .should(QueryBuilders.matchQuery("id", employeeId.toString())));

            String updateCode = "for (entry in params.entrySet()){if (entry.getKey() != \"ctx\") " +
                    "{ctx._source.workPlace[entry.getKey()] = entry.getValue()}}";
            UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest(INDEX_NAME)
              .setRefresh(true)
              .setAbortOnVersionConflict(true)
              .setQuery(boolQueryBuilder)
              .setScript(new Script(ScriptType.INLINE, "painless", updateCode, workPlaceIndexEvent.getWorkplaceIndexMap()));
            highLevelClient.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Listen {@link SavedWorkPlaceIndexEvent} event to create workplace inside {@link EmployeeIndex} index
     * @param workPlaceIndexEvent information about event
     */
    @EventListener(condition = "#workPlaceIndexEvent.getWorkplaceId() == null && !#workPlaceIndexEvent.getEmployeeIds().isEmpty()")
    public void createWorkPlaceInEmployeeIndex(SavedWorkPlaceIndexEvent workPlaceIndexEvent) {
        log.debug("Listening SavedWorkPlaceIndexEvent event to create WorkPlace in EmployeeIndex with WorkPlaceIndex");
        try {
                BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
                workPlaceIndexEvent.getEmployeeIds().forEach(employeeId -> boolQueryBuilder
                        .should(QueryBuilders.matchQuery("id", employeeId.toString())));
            String updateCode = "params.remove(\"ctx\");ctx._source.workPlace=params;";
            UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest(INDEX_NAME)
                    .setRefresh(true)
                    .setAbortOnVersionConflict(true)
                    .setQuery(boolQueryBuilder)
                    .setScript(new Script(ScriptType.INLINE, "painless", updateCode, workPlaceIndexEvent.getWorkplaceIndexMap()));
            highLevelClient.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Get one employee by uid.
     *
     * @param uid the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<EmployeeDTO> getEmployee(UUID uid) {
        log.debug("Request to get Employee : {}", uid);
        return repository.findById(uid).map(mapper::toDto);
    }

    /**
     * Get all the employees.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<EmployeeDTO> getAllEmployees(Pageable pageable) {
        log.debug("Request to get all Employees");
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @EventListener( condition = "#savedNomenclatureEvent.getUpdatedNomenclature() != null")
    public void updateNomenclatureIntoEmployeeIndex(SavedNomenclatureEvent savedNomenclatureEvent) {
        log.debug("Listening SavedNomenclatureEvent event to update Nomenclature with ID {} in EmployeeIndex.",
                savedNomenclatureEvent.getUpdatedNomenclature().getId());
        try {
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            savedNomenclatureEvent.getCommonAssociationIds().forEach(commonAssociationIds -> boolQueryBuilder
                            .should(QueryBuilders.matchQuery("id", commonAssociationIds.toString())));
            if (savedNomenclatureEvent.getUpdatedNomenclature().getDiscriminator().equals(NomenclatureType.CATEGORIA)){
                savedNomenclatureEvent.getUpdatedNomenclature().getEmployeesCategory()
                    .forEach(employee -> boolQueryBuilder
                            .should(QueryBuilders.matchQuery("id", employee.getId().toString())));
            }
            if (savedNomenclatureEvent.getUpdatedNomenclature().getDiscriminator().equals(NomenclatureType.CARGO)){
                savedNomenclatureEvent.getUpdatedNomenclature().getEmployeesCategory()
                        .forEach(employee -> boolQueryBuilder
                                .should(QueryBuilders.matchQuery("id", employee.getId().toString())));
            }
            if (savedNomenclatureEvent.getUpdatedNomenclature().getDiscriminator().equals(NomenclatureType.PROFESION)){
                savedNomenclatureEvent.getUpdatedNomenclature().getEmployeesCategory()
                        .forEach(employee -> boolQueryBuilder
                                .should(QueryBuilders.matchQuery("id", employee.getId().toString())));
            }
            if (savedNomenclatureEvent.getUpdatedNomenclature().getDiscriminator().equals(NomenclatureType.GRADO_CIENTIFICO)){
                savedNomenclatureEvent.getUpdatedNomenclature().getEmployeesCategory()
                        .forEach(employee -> boolQueryBuilder
                                .should(QueryBuilders.matchQuery("id", employee.getId().toString())));
            }

            UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest(INDEX_NAME)
                    .setRefresh(true)
                    .setQuery(boolQueryBuilder)
                    .setAbortOnVersionConflict(true)
                    .setScript(new Script(ScriptType.INLINE, "painless", savedNomenclatureEvent.getUpdateCode(),
                            Collections.emptyMap()));
            highLevelClient.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT);
        } catch (ElasticsearchException | IOException e) {
            e.printStackTrace();
        }
    }

    @EventListener
    public void removeWorkPlaceIntoEmployeeIndex(RemovedWorkPlaceIndexEvent workPlaceIndexEvent) {
        log.debug("Listening RemovedWorkPlaceIndexEvent event to remove WorkPlace in EmployeeIndex with WorkPlaceIndex ID: {}",
                workPlaceIndexEvent.getRemovedWorkPlaceIndexId());
        try {
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            workPlaceIndexEvent.getRemovedWorkPlaceIndex().getEmployees().forEach(employeeId -> boolQueryBuilder
                    .should(QueryBuilders.matchQuery("id", employeeId.toString())));

            UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest(INDEX_NAME)
                    .setRefresh(true)
                    .setAbortOnVersionConflict(true)
                    .setQuery(boolQueryBuilder)
                    .setScript(new Script(ScriptType.INLINE, "painless",
                            "ctx._source.workPlace=null", Collections.emptyMap()));
            highLevelClient.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    /**
     *  Class to register a saved {@link EmployeeIndex} as event
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class SavedEmployeeIndexEvent{
       private String employeeId;
       private Map<String,Object> params;
    }

    /**
     *  Class to register a removed {@link EmployeeIndex} as event
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class RemovedEmployeeIndexEvent{
        private UUID removedEmployeeId;
        private EmployeeIndex removedEmployeeIndex;
        private UUID workPlaceId;
        private List<UUID> phoneIds;
    }
}