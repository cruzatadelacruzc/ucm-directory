package cu.sld.ucmgt.directory.service;

import cu.sld.ucmgt.directory.domain.Employee;
import cu.sld.ucmgt.directory.domain.elasticsearch.EmployeeIndex;
import cu.sld.ucmgt.directory.repository.EmployeeRepository;
import cu.sld.ucmgt.directory.repository.NomenclatureRepository;
import cu.sld.ucmgt.directory.repository.WorkPlaceRepository;
import cu.sld.ucmgt.directory.repository.search.EmployeeSearchRepository;
import cu.sld.ucmgt.directory.service.dto.EmployeeDTO;
import cu.sld.ucmgt.directory.service.mapper.EmployeeIndexMapper;
import cu.sld.ucmgt.directory.service.mapper.EmployeeMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeMapper mapper;
    private final EmployeeRepository repository;
    private final RestHighLevelClient highLevelClient;
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
        params.put("workPlace", null);
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
        repository.deleteById(uid);
        searchRepository.deleteById(uid);
        try {
            String updateCode = "ctx._source.employees.removeIf(employee -> employee.id == \"" + uid.toString() + "\")";
            UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest("workplaces")
                    .setRefresh(true)
                    .setAbortOnVersionConflict(true)
                    .setScript(new Script(ScriptType.INLINE, "painless", updateCode, Collections.emptyMap()));
            highLevelClient.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT);

            DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest("phones")
                    .setRefresh(true)
                    .setAbortOnVersionConflict(true)
                    .setQuery(QueryBuilders.matchQuery("employee.id", uid.toString()));
            highLevelClient.deleteByQuery(deleteByQueryRequest, RequestOptions.DEFAULT);
        } catch (IOException exception) {
            log.error(exception.getMessage());
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
}