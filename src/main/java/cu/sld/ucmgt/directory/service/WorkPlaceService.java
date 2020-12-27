package cu.sld.ucmgt.directory.service;

import cu.sld.ucmgt.directory.domain.Employee;
import cu.sld.ucmgt.directory.domain.Phone;
import cu.sld.ucmgt.directory.domain.WorkPlace;
import cu.sld.ucmgt.directory.repository.EmployeeRepository;
import cu.sld.ucmgt.directory.repository.PhoneRepository;
import cu.sld.ucmgt.directory.repository.WorkPlaceRepository;
import cu.sld.ucmgt.directory.repository.search.WorkPlaceSearchRepository;
import cu.sld.ucmgt.directory.service.dto.WorkPlaceDTO;
import cu.sld.ucmgt.directory.service.mapper.WorkPlaceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkPlaceService {

    private final WorkPlaceMapper mapper;
    private final WorkPlaceRepository repository;
    private final PhoneRepository phoneRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkPlaceSearchRepository searchRepository;

    /**
     * Save a workplace.
     *
     * @param workPlaceDTO the entity to save.
     * @return the persisted entity.
     */
    public WorkPlaceDTO save(WorkPlaceDTO workPlaceDTO) {
        log.debug("Request to save WorkPlace : {}", workPlaceDTO);
        WorkPlace workPlace = mapper.toEntity(workPlaceDTO);
        repository.save(workPlace);
        WorkPlaceDTO result = mapper.toDto(workPlace);
        // find all employees and phones to save in elasticsearch
        if (!workPlaceDTO.getEmployees().isEmpty()){
            Set<Employee> employees = new HashSet<>(employeeRepository.findAllById(workPlaceDTO.getEmployees()));
            workPlace.setEmployees(employees);
        }
        if (!workPlaceDTO.getPhones().isEmpty()){
            Set<Phone> phones = new HashSet<>(phoneRepository.findAllById(workPlaceDTO.getPhones()));
            workPlace.setPhones(phones);
        }
        searchRepository.save(workPlace);
        return result;
    }

    /**
     * Delete the workplace by id.
     *
     * @param uid the id of the entity.
     */
    public void deleteWorkPlace(UUID uid) {
        log.debug("Request to delete WorkPlace : {}", uid);
        repository.deleteById(uid);
        searchRepository.deleteById(uid);
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
