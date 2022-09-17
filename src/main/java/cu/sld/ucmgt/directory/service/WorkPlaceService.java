package cu.sld.ucmgt.directory.service;

import cu.sld.ucmgt.directory.domain.Employee;
import cu.sld.ucmgt.directory.domain.Phone;
import cu.sld.ucmgt.directory.domain.WorkPlace;
import cu.sld.ucmgt.directory.domain.WorkPlace_;
import cu.sld.ucmgt.directory.domain.elasticsearch.WorkPlaceIndex;
import cu.sld.ucmgt.directory.repository.EmployeeRepository;
import cu.sld.ucmgt.directory.repository.PhoneRepository;
import cu.sld.ucmgt.directory.repository.WorkPlaceRepository;
import cu.sld.ucmgt.directory.repository.search.WorkPlaceSearchRepository;
import cu.sld.ucmgt.directory.service.EmployeeService.RemovedEmployeeIndexEvent;
import cu.sld.ucmgt.directory.service.EmployeeService.SavedEmployeeIndexEvent;
import cu.sld.ucmgt.directory.service.PhoneService.RemovedPhoneIndexEvent;
import cu.sld.ucmgt.directory.service.PhoneService.SavedPhoneIndexEvent;
import cu.sld.ucmgt.directory.service.criteria.WorkPlaceCriteria;
import cu.sld.ucmgt.directory.service.dto.WorkPlaceDTO;
import cu.sld.ucmgt.directory.service.mapper.EmployeeMapper;
import cu.sld.ucmgt.directory.service.mapper.PhoneMapper;
import cu.sld.ucmgt.directory.service.mapper.WorkPlaceIndexMapper;
import cu.sld.ucmgt.directory.service.mapper.WorkPlaceMapper;
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

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class WorkPlaceService extends QueryService<WorkPlace>{

    private final WorkPlaceMapper mapper;
    private final PhoneMapper phoneMapper;
    private final EmployeeMapper employeeMapper;
    private final WorkPlaceRepository repository;
    private final PhoneRepository phoneRepository;
    private final RestHighLevelClient highLevelClient;
    private final EmployeeRepository employeeRepository;
    private final WorkPlaceIndexMapper workPlaceIndexMapper;
    private final WorkPlaceSearchRepository searchRepository;
    private final ApplicationEventPublisher eventPublisher;
    private static final String INDEX_NAME = "workplaces";


    /**
     * Create a WorkPlace entity and index WorkPlace
     * @param workPlaceDTO the request data of the workplace to save
     * @param avatar of the user
     * @return the persisted entity.
     */
    public WorkPlaceDTO create(WorkPlaceDTO workPlaceDTO, MultipartFile avatar) {
        WorkPlace workPlace = mapper.toEntity(workPlaceDTO);

        // find all employees and phones to saves
        loadAssociations(workPlaceDTO, workPlace);

        repository.save(workPlace);

        String fileName = getFileName(workPlace, avatar);
        if (avatar != null) {
            workPlace.setAvatarUrl(fileName);
        }

        if (avatar != null) {
            final FileService.SaveFileEvent saveFileEvent = FileService.SaveFileEvent.builder()
                    .newFileName(fileName)
                    .fileInput(avatar)
                    .build();
            eventPublisher.publishEvent(saveFileEvent);
        }

        saveWorkPlaceIndex(workPlace, true);
        return mapper.toDto(workPlace);
    }

    /**
     * Update un WorkPlace and WorkPlaceIndex
     * @param workPlaceDTO the data of the workplace to save
     * @param file Avatar
     * @return the persisted entity.
     */
    public WorkPlaceDTO update(WorkPlaceDTO workPlaceDTO, MultipartFile file) {
        String oldFileName = "";

        WorkPlace workPlaceFetched = repository.findWorkPlaceWithAssociationsById(workPlaceDTO.getId())
                .orElseThrow(() -> new NoSuchElementException("WorkPlaceIndex with ID:" + workPlaceDTO.getId() + " not was found"));

        // remove all associations for updating the new associations
        if (!workPlaceDTO.getPhoneIds().isEmpty() || !workPlaceDTO.getEmployeeIds().isEmpty()) {
            if (workPlaceDTO.getEmployeeIds() != null && !workPlaceDTO.getEmployeeIds().isEmpty()) {
                    new HashSet<>(workPlaceFetched.getEmployees()).forEach(workPlaceFetched::removeEmployee);
                }
                if (workPlaceDTO.getPhoneIds() != null && !workPlaceDTO.getPhoneIds().isEmpty()) {
                    new HashSet<>(workPlaceFetched.getPhones()).forEach(workPlaceFetched::removePhone);
                }
            }


        // find all employees and phones to saves
        loadAssociations(workPlaceDTO, workPlaceFetched);

        workPlaceFetched.setName(workPlaceDTO.getName());
        workPlaceFetched.setEmail(workPlaceDTO.getEmail());
        boolean oldStatus = workPlaceFetched.getActive();
        workPlaceFetched.setActive(workPlaceDTO.getActive());
        workPlaceFetched.setDescription(workPlaceDTO.getDescription());

        String newFileName = getFileName(workPlaceFetched, file);
        // case: For renaming or updating a exists avatar
        if (workPlaceFetched.getAvatarUrl() != null) {
            oldFileName = workPlaceFetched.getAvatarUrl();
            String extension = Optional.ofNullable(FilenameUtils.getExtension(oldFileName)).orElse("");
            newFileName = !extension.isBlank() ? newFileName + "." + extension : newFileName;
        }

        // case: To store new avatar or update a exists avatar
        if (file != null || (!newFileName.equals(oldFileName) && !oldFileName.isBlank())) {
            workPlaceFetched.setAvatarUrl(newFileName);
        }

        repository.save(workPlaceFetched);


        final FileService.SaveFileEvent saveFileEvent = FileService.SaveFileEvent.builder()
                    .newFileName(newFileName)
                    .oldFileName(oldFileName)
                    .fileInput(file)
                    .build();
            eventPublisher.publishEvent(saveFileEvent);

        if ((workPlaceDTO.getActive() && !oldStatus) || (!workPlaceDTO.getActive() && oldStatus)) {
            switchStatus(workPlaceFetched, workPlaceDTO.getActive());
        } else {
            saveWorkPlaceIndex(workPlaceFetched, false);
        }
        return mapper.toDto(workPlaceFetched);
    }

    /**
     * Set a new File Name
     * @param workPlace {@link WorkPlace} entity
     * @param file avatar with content type image/png or image/jpeg
     * @return new file name
     */
    private String getFileName(WorkPlace workPlace, MultipartFile file) {
        String newFileName = workPlace.getName().replaceAll("[^a-zA-Z0-9_-]","").toLowerCase();
        newFileName =  newFileName + "@" + workPlace.getId().toString();
        if (file != null) {
            newFileName = ServiceUtils.getAvatarNameWithExtension(file, newFileName.toLowerCase());
        }

        return newFileName.toLowerCase();
    }

    /**
     * Update a WorkPlace entity and index WorkPlace
     * @param workPlace persisted entity to save as WorkPlaceIndex
     * @param isNew flag indicating whether to update or create WorkplaceIndex
     */
    private void saveWorkPlaceIndex(WorkPlace workPlace, boolean isNew) {
        WorkPlaceIndex workPlaceIndex = workPlaceIndexMapper.toIndex(workPlace);
        searchRepository.save(workPlaceIndex);
        // saving the workplace belonging to phones and employees
        Map<String, Object> workPlaceMap = convertWorkPlaceIndexToWorkPlaceIndexMap(workPlaceIndex);
        List<UUID> employeeIds = workPlace.getEmployees().stream().map(Employee::getId).collect(Collectors.toList());
        List<UUID> phoneIds = workPlace.getPhones().stream().map(Phone::getId).collect(Collectors.toList());
        final SavedWorkPlaceIndexEvent savedWorkPlaceIndexEvent = SavedWorkPlaceIndexEvent.builder()
                .workplaceId(isNew ? null: workPlaceIndex.getId())
                .workplaceIndexMap(workPlaceMap)
                .employeeIds(employeeIds)
                .phoneIds(phoneIds)
                .build();
        eventPublisher.publishEvent(savedWorkPlaceIndexEvent);
    }

    /**
     * Add all associations (Phones and Employees) to WorkPlace
     * @param workPlaceDTO the request data with EmployeeIds and PhoneIds
     * @param workPlace persistent entity to add all associations
     */
    private void loadAssociations(WorkPlaceDTO workPlaceDTO, WorkPlace workPlace) {
        if (workPlaceDTO.getEmployeeIds() != null) {
            Set<Employee> employees = workPlaceDTO.getEmployeeIds().stream()
                    .map(employeeRepository::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
            employees.forEach(workPlace::addEmployee);
        }
        if (workPlaceDTO.getPhoneIds() != null) {
            Set<Phone> phones = workPlaceDTO.getPhoneIds().stream()
                    .map(phoneRepository::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
            phones.forEach(workPlace::addPhone);
        }
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
        workPlaceIndexMap.put("avatarUrl", workPlaceIndex.getAvatarUrl());
        workPlaceIndexMap.put("description", workPlaceIndex.getDescription());
        return workPlaceIndexMap;
    }

    @EventListener(condition = "#employeeIndexEvent.getParams().get(\"workPlace\") != null")
    public void saveEmployeeIntoWorkPlaceIndex(SavedEmployeeIndexEvent employeeIndexEvent) {
        log.debug("Listening SavedEmployeeIndexEvent event to save EmployeeIndex with ID: {} in WorkPlaceIndex",
                employeeIndexEvent.getEmployeeId());
        Object workPlaceMap = employeeIndexEvent.getParams().get("workPlace");
            try {
                String workPlaceId = (String) ((HashMap) workPlaceMap).get("id");
                // avoid redundant data, employee.workplace equals current workplace
                employeeIndexEvent.getParams().replace("workPlace", null);
                String updateCode = "params.remove(\"ctx\");ctx._source.employees.add(params)";
                if (employeeIndexEvent.getEmployeeId() != null) {
                    updateCode = "def targets = ctx._source.employees.findAll(employee " +
                            "-> employee.id == \"" + employeeIndexEvent.getEmployeeId() + "\" ); " +
                            "if (targets.length == 0) {params.remove(\"ctx\");ctx._source.employees.add(params)}" +
                            "else { for (employee in targets) { for (entry in params.entrySet()) { if (entry.getKey() != \"ctx\") {" +
                            "employee[entry.getKey()] = entry.getValue() }}}}";
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

    @EventListener(condition = "#phoneIndexEvent.getWorkPlaceId() != null")
    public void savePhoneInWorkPlaceIndex(SavedPhoneIndexEvent phoneIndexEvent) {
        log.debug("Listening SavedPhoneIndexEvent event to save Phone into WorkPlaceIndex with ID: {}",
                phoneIndexEvent.getWorkPlaceId());
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

    @EventListener(condition = "#event.getWorkPlaceId() != null")
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

    @EventListener(condition = "#event.getWorkPlaceId() != null ")
    public void removeEmployeeIndexIntoWorkPlaceIndex(RemovedEmployeeIndexEvent event) {
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
       return repository
                .findWorkPlaceWithAssociationsById(uid)
                .map(workPlace -> {
                    WorkPlaceDTO workPlaceDTO = mapper.toDto(workPlace);
                    workPlaceDTO.setEmployees(employeeMapper.toDtos(workPlace.getEmployees()));
                    workPlaceDTO.setPhones(phoneMapper.toDtos(workPlace.getPhones()));
                    return workPlaceDTO;
                });
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
     * Toggle status for active or disable workplace
     *
     * @param id          workplace identifier
     * @param status true or false
     * @return true if changed status or false otherwise
     */
    public Boolean changeStatus(UUID id, Boolean status) {
        Optional<WorkPlace> workPlaceToUpdate  = repository.findWorkPlaceWithAssociationsById(id);
        if (workPlaceToUpdate.isPresent()){
            workPlaceToUpdate.get().setActive(status);
            repository.save(workPlaceToUpdate.get());
            switchStatus(workPlaceToUpdate.get(), status);
            return true;
        }
        return false;
    }

    /**
     * Toggle the status of WorkPlaceIndex, if the status is false then WorkPlaceIndex must be removed;
     * otherwise WorkPlaceIndex must be created
     *
     * @param workPlaceToUpdate persisted entity
     * @param status request WorkPlace's status
     */
    private void switchStatus(WorkPlace workPlaceToUpdate, boolean status) {
        if (status) {
            // WorkPlaceIndex must to be created because when WorkPlace was disabled, WorkPlaceIndex was removed
            saveWorkPlaceIndex(workPlaceToUpdate, true);
        } else {
            // WorkPlaceIndex must to be removed because WorkPlace was disabled
            WorkPlaceIndex workPlaceIndex = searchRepository.findById(workPlaceToUpdate.getId())
                    .orElseThrow(() -> new NoSuchElementException("WorkPlaceIndex with ID: "
                            + workPlaceToUpdate.getId() + " not was found"));
            searchRepository.delete(workPlaceIndex);
            final RemovedWorkPlaceIndexEvent removedWorkPlaceIndexEvent = RemovedWorkPlaceIndexEvent.builder()
                    .removedWorkPlaceIndexId(workPlaceIndex.getId())
                    .removedWorkPlaceIndex(workPlaceIndex)
                    .build();
            eventPublisher.publishEvent(removedWorkPlaceIndexEvent);
        }
    }

    /**
     * Return a {@link List} of {@link WorkPlaceDTO} which matches the criteria from the database.
     *
     * @param join Logical operator to join expression: AND - OR
     * @param criteria       The object which holds all the filters, which the entities should match.
     * @return the matching entities.
     */
    public Page<WorkPlaceDTO> findByCriteria(String join, WorkPlaceCriteria criteria, Pageable pageable) {
        final Specification<WorkPlace> specification = createSpecification(join, criteria);
        return repository.findAll(specification, pageable).map(mapper::toDto);
    }

    /**
     * Function to convert {@link WorkPlaceCriteria} to a {@link Specification}
     * @param join Logical operator to join expression: AND - OR
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    private Specification<WorkPlace> createSpecification(String join, WorkPlaceCriteria criteria) {
        Specification<WorkPlace> specification = Specification.where(null);
        if (criteria != null) {
            if (join.equalsIgnoreCase("AND")) {
                if (criteria.getId() != null) {
                    specification = specification.and(buildSpecification(criteria.getId(), WorkPlace_.id));
                }
                if (criteria.getActive() != null) {
                    specification = specification.and(buildSpecification(criteria.getActive(), WorkPlace_.active));
                }
                if (criteria.getName() != null) {
                    specification = specification.and(buildStringSpecification(criteria.getName(), WorkPlace_.name));
                }
                if (criteria.getEmail() != null) {
                    specification = specification.and(buildStringSpecification(criteria.getEmail(), WorkPlace_.email));
                }
                if (criteria.getDescription() != null) {
                    specification = specification.and(buildStringSpecification(criteria.getDescription(), WorkPlace_.description));
                }
            } else {
                if (criteria.getId() != null) {
                    specification = specification.or(buildSpecification(criteria.getId(), WorkPlace_.id));
                }
                if (criteria.getActive() != null) {
                    specification = specification.or(buildSpecification(criteria.getActive(), WorkPlace_.active));
                }
                if (criteria.getName() != null) {
                    specification = specification.or(buildStringSpecification(criteria.getName(), WorkPlace_.name));
                }
                if (criteria.getEmail() != null) {
                    specification = specification.or(buildStringSpecification(criteria.getEmail(), WorkPlace_.email));
                }
                if (criteria.getDescription() != null) {
                    specification = specification.or(buildStringSpecification(criteria.getDescription(), WorkPlace_.description));
                }
            }
        }
        return specification;
    }

    /**
     * Delete avatar of workplace
     * @param workplaceId identifier of {@link WorkPlace}
     * @return True if avatar was removed False otherwise
     */
    public Boolean deleteAvatar(UUID workplaceId) {
        Optional<WorkPlace> workPlaceOptional = repository.findById(workplaceId);
        if (workPlaceOptional.isPresent()) {
            String avatar = workPlaceOptional.get().getAvatarUrl();
            workPlaceOptional.get().setAvatarUrl(null);
            repository.save(workPlaceOptional.get());
            final FileService.DeleteFileEvent deleteFileEvent = FileService.DeleteFileEvent.builder()
                    .fileName(avatar)
                    .build();
            eventPublisher.publishEvent(deleteFileEvent);
            return true;
        }
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
