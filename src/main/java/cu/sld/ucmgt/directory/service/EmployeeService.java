package cu.sld.ucmgt.directory.service;

import cu.sld.ucmgt.directory.domain.*;
import cu.sld.ucmgt.directory.domain.elasticsearch.EmployeeIndex;
import cu.sld.ucmgt.directory.repository.EmployeeRepository;
import cu.sld.ucmgt.directory.repository.NomenclatureRepository;
import cu.sld.ucmgt.directory.repository.WorkPlaceRepository;
import cu.sld.ucmgt.directory.repository.search.EmployeeSearchRepository;
import cu.sld.ucmgt.directory.service.FileService.DeleteFileEvent;
import cu.sld.ucmgt.directory.service.FileService.SaveFileEvent;
import cu.sld.ucmgt.directory.service.NomenclatureService.SavedNomenclatureEvent;
import cu.sld.ucmgt.directory.service.WorkPlaceService.RemovedWorkPlaceIndexEvent;
import cu.sld.ucmgt.directory.service.WorkPlaceService.SavedWorkPlaceIndexEvent;
import cu.sld.ucmgt.directory.service.criteria.EmployeeCriteria;
import cu.sld.ucmgt.directory.service.dto.EmployeeDTO;
import cu.sld.ucmgt.directory.service.mapper.EmployeeIndexMapper;
import cu.sld.ucmgt.directory.service.mapper.EmployeeMapper;
import cu.sld.ucmgt.directory.service.mapper.PhoneMapper;
import cu.sld.ucmgt.directory.service.utils.ServiceUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.JoinType;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EmployeeService extends QueryService<Employee> {

    private final EmployeeMapper mapper;
    private final PhoneMapper phoneMapper;
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
     * @param employee the entity to save.
     * @return the persisted entity.
     */
    public Employee save(Employee employee) {
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
     * @param avatar of user
     * @return the persisted entity.
     */
    public EmployeeDTO create(EmployeeDTO employeeDTO, MultipartFile avatar) {
        Employee employee = mapper.toEntity(employeeDTO);
        employee = this.save(employee);
        String fileName = ServiceUtils.buildAvatarName(employee);
        if (avatar != null) {
            fileName = ServiceUtils.getAvatarNameWithExtension(avatar, fileName.toLowerCase());
            employee.setAvatarUrl(fileName);
        }
        EmployeeIndex employeeIndex = employeeIndexMapper.toIndex(employee);
        searchRepository.save(employeeIndex);
        // saving the EmployeeIndex belonging to PhoneIndex and WorkPlaceIndex
        Map<String, Object> employeeIndexMap = createEmployeeToEmployeeIndexMap(employee);
        final SavedEmployeeIndexEvent savedEmployeeIndexEvent = SavedEmployeeIndexEvent.builder()
                .params(employeeIndexMap)
                .build();
        eventPublisher.publishEvent(savedEmployeeIndexEvent);
        if (avatar != null) {
            final SaveFileEvent saveFileEvent = SaveFileEvent.builder()
                    .newFileName(fileName)
                    .fileInput(avatar)
                    .build();
            eventPublisher.publishEvent(saveFileEvent);
        }
        return mapper.toDto(employee);
    }

    /**
     * Update a employee and employee inside phone and workplace.
     *
     * @param employeeDTO the entity to save.
     * @return the persisted entity.
     */
    public EmployeeDTO update(EmployeeDTO employeeDTO, MultipartFile avatar) {
        String oldFileName = "";
        UUID employeeIdToRemove = null;
        UUID workPlaceIdRemoved = null;
        Employee employee = mapper.toEntity(employeeDTO);
        String newFileName = ServiceUtils.buildAvatarName(employee);
        if (avatar != null) {
            newFileName = ServiceUtils.getAvatarNameWithExtension(avatar, newFileName);
        }

        Optional<Employee> optionalEmployee = repository.findById(employeeDTO.getId());
        if (optionalEmployee.isPresent()) {
            Employee employeeFetched = optionalEmployee.get();
            // set avatarUrl to the new employee for avoid erase stored data
            employee.setAvatarUrl(employeeFetched.getAvatarUrl());
            // case: For renaming or updating a exists avatar
            if (employeeFetched.getAvatarUrl() != null) {
                oldFileName = employeeFetched.getAvatarUrl();
                String extension = Optional.ofNullable(FilenameUtils.getExtension(oldFileName)).orElse("");
                newFileName = !extension.isBlank() ? newFileName + "." + extension : newFileName;
            }

            // if employee should be removed own workplace
            if (employeeFetched.getWorkPlace() != null) {
                employeeIdToRemove = employeeFetched.getId();
                workPlaceIdRemoved = employeeFetched.getWorkPlace().getId();
            }
        }

        // case: To store new avatar or update a exists avatar
        if (avatar != null || (!newFileName.equals(oldFileName) && !oldFileName.isBlank())) {
            employee.setAvatarUrl(newFileName);
        }

        employee = this.save(employee);
        searchRepository.save(employeeIndexMapper.toIndex(employee));

        // updating the EmployeeIndex belonging to PhoneIndex and WorkPlaceIndex
        Map<String, Object> employeeIndexMap = createEmployeeToEmployeeIndexMap(employee);
        if (employeeIdToRemove != null) {
            final RemovedEmployeeIndexEvent removedEmployeeIndexEvent = RemovedEmployeeIndexEvent.builder()
                    .workPlaceId(workPlaceIdRemoved)
                    .removedEmployeeId(employeeIdToRemove)
                    .build();
            eventPublisher.publishEvent(removedEmployeeIndexEvent);
        }
        final SaveFileEvent saveFileEvent = SaveFileEvent.builder()
                .newFileName(newFileName)
                .oldFileName(oldFileName)
                .fileInput(avatar)
                .build();
        final SavedEmployeeIndexEvent savedEmployeeIndexEvent = SavedEmployeeIndexEvent.builder()
                .employeeId(employee.getId().toString())
                .params(employeeIndexMap)
                .build();
        eventPublisher.publishEvent(saveFileEvent);
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
        params.put("race", employee.getRace());
        params.put("name", employee.getName());
        params.put("email", employee.getEmail());
        params.put("gender", employee.getGender());
        params.put("address", employee.getAddress());
        params.put("id", employee.getId().toString());
        params.put("avatarUrl", employee.getAvatarUrl());
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
        repository.findEmployeeWithAssociationsById(uid).ifPresent(employee -> {
            List<UUID> phoneIds = employee.getPhones().stream().map(Phone::getId).collect(Collectors.toList());
            new HashSet<>(employee.getPhones()).forEach(employee::removePhone);
            String avatar = employee.getAvatarUrl();
            repository.delete(employee);
            EmployeeIndex employeeIndex = searchRepository.findById(employee.getId())
                    .orElseThrow(() -> new NoSuchElementException("EmployeeIndex with ID: " + employee.getId() + " not was found"));
            searchRepository.delete(employeeIndex);

            final RemovedEmployeeIndexEvent removedEmployeeIndexEvent = RemovedEmployeeIndexEvent.builder()
                    .phoneIds(phoneIds)
                    .removedEmployeeIndex(employeeIndex)
                    .removedEmployeeId(employeeIndex.getId())
                    .workPlaceId(employee.getWorkPlace() != null ? employee.getWorkPlace().getId() : null)
                    .build();
            final DeleteFileEvent deleteFileEvent = DeleteFileEvent.builder()
                    .fileName(avatar)
                    .build();

            eventPublisher.publishEvent(deleteFileEvent);
            eventPublisher.publishEvent(removedEmployeeIndexEvent);
        });
    }

    /**
     * Listen {@link SavedWorkPlaceIndexEvent} event to update workplace inside {@link EmployeeIndex} index
     *
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

            String updateCode = "if (ctx._source.workPlace == null) {params.remove(\"ctx\");ctx._source.workPlace=params;}" +
                    "else { for (entry in params.entrySet()){if (entry.getKey() != \"ctx\") " +
                    "{ctx._source.workPlace[entry.getKey()] = entry.getValue()}}}";
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
     *
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
        return repository.findEmployeeWithAssociationsById(uid).map(employee -> {
            EmployeeDTO employeeDTO = mapper.toDto(employee);
            employeeDTO.setPhones(phoneMapper.toDtos(employee.getPhones()));
            return employeeDTO;
        });
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
     * Add employeeIds to filter of employee(employee with district association or specialty added above) that need
     * to be updated
     * @param savedNomenclatureEvent with saved employee data
     */
    @EventListener(condition = "#savedNomenclatureEvent.getUpdatedNomenclature() != null")
    public void updateNomenclatureIntoEmployeeIndex(SavedNomenclatureEvent savedNomenclatureEvent) {
        log.debug("Listening SavedNomenclatureEvent event to update Nomenclature with ID {} in EmployeeIndex.",
                savedNomenclatureEvent.getUpdatedNomenclature().getId());
        try {
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            savedNomenclatureEvent.getCommonAssociationIds().forEach(commonAssociationIds -> boolQueryBuilder
                    .should(QueryBuilders.matchQuery("id", commonAssociationIds.toString())));
            if (savedNomenclatureEvent.getUpdatedNomenclature().getDiscriminator().equals(NomenclatureType.CATEGORIA)) {
                savedNomenclatureEvent.getUpdatedNomenclature().getEmployeesCategory()
                        .forEach(employee -> boolQueryBuilder
                                .should(QueryBuilders.matchQuery("id", employee.getId().toString())));
            }
            if (savedNomenclatureEvent.getUpdatedNomenclature().getDiscriminator().equals(NomenclatureType.CARGO)) {
                savedNomenclatureEvent.getUpdatedNomenclature().getEmployeesCategory()
                        .forEach(employee -> boolQueryBuilder
                                .should(QueryBuilders.matchQuery("id", employee.getId().toString())));
            }
            if (savedNomenclatureEvent.getUpdatedNomenclature().getDiscriminator().equals(NomenclatureType.PROFESION)) {
                savedNomenclatureEvent.getUpdatedNomenclature().getEmployeesCategory()
                        .forEach(employee -> boolQueryBuilder
                                .should(QueryBuilders.matchQuery("id", employee.getId().toString())));
            }
            if (savedNomenclatureEvent.getUpdatedNomenclature().getDiscriminator().equals(NomenclatureType.GRADO_CIENTIFICO)) {
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
     * Return a {@link List} of {@link EmployeeDTO} which matches the criteria from the database.
     *
     * @param operator_union Logical operator to join expression: AND - OR
     * @param criteria       The object which holds all the filters, which the entities should match.
     * @return the matching entities.
     */
    public Page<EmployeeDTO> findByCriteria(String operator_union, EmployeeCriteria criteria, Pageable page) {
        final Specification<Employee> specification = createSpecification(operator_union, criteria);
        return repository.findAll(specification, page).map(mapper::toDto);
    }

    /**
     * Function to convert {@link EmployeeCriteria} to a {@link Specification}
     *
     * @param operator_union Logical operator to join expression: AND - OR
     * @param criteria       The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    private Specification<Employee> createSpecification(String operator_union, EmployeeCriteria criteria) {
        Specification<Employee> specification = Specification.where(null);
        if (criteria != null) {
            if (operator_union.equalsIgnoreCase("AND")) {
                if (criteria.getId() != null) {
                    specification = specification.and(buildSpecification(criteria.getId(), Employee_.id));
                }
                if (criteria.getCi() != null) {
                    specification = specification.and(buildStringSpecification(criteria.getCi(), Employee_.ci));
                }
                if (criteria.getName() != null) {
                    specification = specification.and(buildStringSpecification(criteria.getName(), Employee_.name));
                }
                if (criteria.getRace() != null) {
                    specification = specification.and(buildStringSpecification(criteria.getRace(), Employee_.race));
                }
                if (criteria.getEmail() != null) {
                    specification = specification.and(buildStringSpecification(criteria.getEmail(), Employee_.email));
                }
                if (criteria.getGender() != null) {
                    specification = specification.and(buildSpecification(criteria.getGender(), Employee_.gender));
                }
                if (criteria.getAddress() != null) {
                    specification = specification.and(buildStringSpecification(criteria.getAddress(), Employee_.address));
                }
                if (criteria.getDistrictName() != null) {
                    specification = specification.and(buildSpecification(criteria.getDistrictName(),
                            root -> root.join(Employee_.district, JoinType.LEFT).get(Nomenclature_.name)));
                }
                if (criteria.getBirthdate() != null) {
                    specification = specification.and(buildRangeSpecification(criteria.getBirthdate(), Employee_.birthdate));
                }
                if (criteria.getSpecialtyName() != null) {
                    specification = specification.and(buildSpecification(criteria.getSpecialtyName(),
                            root -> root.join(Employee_.specialty, JoinType.LEFT).get(Nomenclature_.name)));
                }
                if (criteria.getFirstLastName() != null) {
                    specification = specification.and(buildStringSpecification(criteria.getFirstLastName(), Employee_.firstLastName));
                }
                if (criteria.getWorkPlaceId() != null) {
                    specification = specification.and(buildSpecification(criteria.getWorkPlaceId(),
                            root -> root.join(Employee_.workPlace, JoinType.LEFT).get(WorkPlace_.id)));
                }
                if (criteria.getSecondLastName() != null) {
                    specification = specification.and(buildStringSpecification(criteria.getSecondLastName(), Employee_.secondLastName));
                }
                if (criteria.getBossWorkPlace() != null) {
                    specification = specification.and(buildSpecification(criteria.getBossWorkPlace(), Employee_.bossWorkPlace));
                }
                if (criteria.getCategoryName() != null) {
                    specification = specification.and(buildSpecification(criteria.getCategoryName(),
                            root -> root.join(Employee_.category).get(Nomenclature_.name)));
                }
                if (criteria.getChargeName() != null) {
                    specification = specification.and(buildSpecification(criteria.getChargeName(),
                            root -> root.join(Employee_.charge, JoinType.LEFT).get(Nomenclature_.name)));
                }
                if (criteria.getEndDate() != null) {
                    specification = specification.and(buildRangeSpecification(criteria.getEndDate(), Employee_.endDate));
                }
                if (criteria.getGraduateYears() != null) {
                    specification = specification.and(buildRangeSpecification(criteria.getGraduateYears(), Employee_.graduateYears));
                }
                if (criteria.getIsGraduatedBySector() != null) {
                    specification = specification.and(buildSpecification(criteria.getIsGraduatedBySector(), Employee_.isGraduatedBySector));
                }
                if (criteria.getProfessionalNumber() != null) {
                    specification = specification.and(buildStringSpecification(criteria.getProfessionalNumber(), Employee_.professionalNumber));
                }
                if (criteria.getProfessionName() != null) {
                    specification = specification.and(buildSpecification(criteria.getProfessionName(),
                            root -> root.join(Employee_.profession, JoinType.LEFT).get(Nomenclature_.name)));
                }
                if (criteria.getRegisterNumber() != null) {
                    specification = specification.and(buildStringSpecification(criteria.getRegisterNumber(), Employee_.registerNumber));
                }
                if (criteria.getScientificDegreeName() != null) {
                    specification = specification.and(buildSpecification(criteria.getScientificDegreeName(),
                            root -> root.join(Employee_.scientificDegree, JoinType.LEFT).get(Nomenclature_.name)));
                }
                if (criteria.getServiceYears() != null) {
                    specification = specification.and(buildRangeSpecification(criteria.getServiceYears(), Employee_.serviceYears));
                }
                if (criteria.getStartDate() != null) {
                    specification = specification.and(buildRangeSpecification(criteria.getStartDate(), Employee_.startDate));
                }
                if (criteria.getTeachingCategoryName() != null) {
                    specification = specification.and(buildSpecification(criteria.getTeachingCategoryName(),
                            root -> root.join(Employee_.teachingCategory, JoinType.LEFT).get(Nomenclature_.name)));
                }
                if (criteria.getWorkPlaceName() != null) {
                    specification = specification.and(buildSpecification(criteria.getWorkPlaceName(),
                            root -> root.join(Employee_.workPlace, JoinType.LEFT).get(WorkPlace_.name)));
                }
            } else {
                if (criteria.getId() != null) {
                    specification = specification.or(buildSpecification(criteria.getId(), Employee_.id));
                }
                if (criteria.getCi() != null) {
                    specification = specification.or(buildStringSpecification(criteria.getCi(), Employee_.ci));
                }
                if (criteria.getName() != null) {
                    specification = specification.or(buildStringSpecification(criteria.getName(), Employee_.name));
                }
                if (criteria.getRace() != null) {
                    specification = specification.or(buildStringSpecification(criteria.getRace(), Employee_.race));
                }
                if (criteria.getEmail() != null) {
                    specification = specification.or(buildStringSpecification(criteria.getEmail(), Employee_.email));
                }
                if (criteria.getWorkPlaceId() != null) {
                    specification = specification.or(buildSpecification(criteria.getWorkPlaceId(),
                            root -> root.join(Employee_.workPlace, JoinType.LEFT).get(WorkPlace_.id)));
                }
                if (criteria.getGender() != null) {
                    specification = specification.or(buildSpecification(criteria.getGender(), Employee_.gender));
                }
                if (criteria.getAddress() != null) {
                    specification = specification.or(buildStringSpecification(criteria.getAddress(), Employee_.address));
                }
                if (criteria.getDistrictName() != null) {
                    specification = specification.or(buildSpecification(criteria.getDistrictName(),
                            root -> root.join(Employee_.district, JoinType.LEFT).get(Nomenclature_.name)));
                }
                if (criteria.getBirthdate() != null) {
                    specification = specification.or(buildRangeSpecification(criteria.getBirthdate(), Employee_.birthdate));
                }
                if (criteria.getSpecialtyName() != null) {
                    specification = specification.or(buildSpecification(criteria.getSpecialtyName(),
                            root -> root.join(Employee_.specialty, JoinType.LEFT).get(Nomenclature_.name)));
                }
                if (criteria.getFirstLastName() != null) {
                    specification = specification.or(buildStringSpecification(criteria.getFirstLastName(), Employee_.firstLastName));
                }
                if (criteria.getSecondLastName() != null) {
                    specification = specification.or(buildStringSpecification(criteria.getSecondLastName(), Employee_.secondLastName));
                }
                if (criteria.getBossWorkPlace() != null) {
                    specification = specification.or(buildSpecification(criteria.getBossWorkPlace(), Employee_.bossWorkPlace));
                }
                if (criteria.getCategoryName() != null) {
                    specification = specification.or(buildSpecification(criteria.getCategoryName(),
                            root -> root.join(Employee_.category).get(Nomenclature_.name)));
                }
                if (criteria.getChargeName() != null) {
                    specification = specification.or(buildSpecification(criteria.getChargeName(),
                            root -> root.join(Employee_.charge, JoinType.LEFT).get(Nomenclature_.name)));
                }
                if (criteria.getEndDate() != null) {
                    specification = specification.or(buildRangeSpecification(criteria.getEndDate(), Employee_.endDate));
                }
                if (criteria.getGraduateYears() != null) {
                    specification = specification.or(buildRangeSpecification(criteria.getGraduateYears(), Employee_.graduateYears));
                }
                if (criteria.getIsGraduatedBySector() != null) {
                    specification = specification.or(buildSpecification(criteria.getIsGraduatedBySector(), Employee_.isGraduatedBySector));
                }
                if (criteria.getProfessionalNumber() != null) {
                    specification = specification.or(buildStringSpecification(criteria.getProfessionalNumber(), Employee_.professionalNumber));
                }
                if (criteria.getProfessionName() != null) {
                    specification = specification.or(buildSpecification(criteria.getProfessionName(),
                            root -> root.join(Employee_.profession, JoinType.LEFT).get(Nomenclature_.name)));
                }
                if (criteria.getRegisterNumber() != null) {
                    specification = specification.or(buildStringSpecification(criteria.getRegisterNumber(), Employee_.registerNumber));
                }
                if (criteria.getScientificDegreeName() != null) {
                    specification = specification.or(buildSpecification(criteria.getScientificDegreeName(),
                            root -> root.join(Employee_.scientificDegree, JoinType.LEFT).get(Nomenclature_.name)));
                }
                if (criteria.getServiceYears() != null) {
                    specification = specification.or(buildRangeSpecification(criteria.getServiceYears(), Employee_.serviceYears));
                }
                if (criteria.getStartDate() != null) {
                    specification = specification.or(buildRangeSpecification(criteria.getStartDate(), Employee_.startDate));
                }
                if (criteria.getTeachingCategoryName() != null) {
                    specification = specification.or(buildSpecification(criteria.getTeachingCategoryName(),
                            root -> root.join(Employee_.teachingCategory, JoinType.LEFT).get(Nomenclature_.name)));
                }
                if (criteria.getWorkPlaceName() != null) {
                    specification = specification.or(buildSpecification(criteria.getWorkPlaceName(),
                            root -> root.join(Employee_.workPlace, JoinType.LEFT).get(WorkPlace_.name)));
                }
            }
        }
        return specification;
    }

    /**
     * Delete avatar of employee
     * @param employeeId identifier of employee
     */
    public Boolean deleteAvatar(UUID employeeId) {
        Optional<Employee> optionalEmployee =  repository.findById(employeeId);
        if (optionalEmployee.isPresent()) {
            String avatar = optionalEmployee.get().getAvatarUrl();
            optionalEmployee.get().setAvatarUrl(null);
            repository.save(optionalEmployee.get());
            final DeleteFileEvent deleteFileEvent = DeleteFileEvent.builder()
                    .fileName(avatar)
                    .build();
            eventPublisher.publishEvent(deleteFileEvent);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Class to register a saved {@link EmployeeIndex} as event
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class SavedEmployeeIndexEvent {
        private String employeeId;
        private Map<String, Object> params;
    }

    /**
     * Class to register a removed {@link EmployeeIndex} as event
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class RemovedEmployeeIndexEvent {
        private UUID removedEmployeeId;
        private EmployeeIndex removedEmployeeIndex;
        private UUID workPlaceId;
        private List<UUID> phoneIds;
    }
}