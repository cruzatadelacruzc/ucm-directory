package cu.sld.ucmgt.directory.service;

import cu.sld.ucmgt.directory.domain.Employee;
import cu.sld.ucmgt.directory.domain.Phone;
import cu.sld.ucmgt.directory.domain.WorkPlace;
import cu.sld.ucmgt.directory.repository.EmployeeRepository;
import cu.sld.ucmgt.directory.repository.PhoneRepository;
import cu.sld.ucmgt.directory.repository.WorkPlaceRepository;
import cu.sld.ucmgt.directory.repository.search.EmployeeSearchRepository;
import cu.sld.ucmgt.directory.repository.search.PhoneSearchRepository;
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
    private final EmployeeRepository employeeRepository;
    private final WorkPlaceSearchRepository searchRepository;
    private final PhoneSearchRepository phoneSearchRepository;
    private final EmployeeSearchRepository employeeSearchRepository;

    /**
     * Save a workplace.
     *
     * @param workPlaceDTO the entity to save.
     * @return the persisted entity.
     */
    public WorkPlaceDTO save(WorkPlaceDTO workPlaceDTO) {
        log.debug("Request to save WorkPlace : {}", workPlaceDTO);
        WorkPlace workPlace = mapper.toEntity(workPlaceDTO);
        // find all employees to save in elasticsearch
        if (workPlaceDTO.getEmployees() != null) {
            Set<Employee> employees = workPlaceDTO.getEmployees().stream()
                    .map(employeeRepository::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
            workPlace.setEmployees(employees);
        }
        repository.save(workPlace);
        // updating the workplace belonging to phones and employees
        if (workPlaceDTO.getId() != null) {
            List<Phone> updatedWorkplacePhones = phoneSearchRepository.findAllByWorkPlace_Id(workPlaceDTO.getId()).stream()
                    .peek(phone -> phone.setWorkPlace(workPlace)).collect(Collectors.toList());
            if (!updatedWorkplacePhones.isEmpty()) {
                phoneSearchRepository.saveAll(updatedWorkplacePhones);
            }

            List<Employee> updatedWorkplaceEmployees = employeeSearchRepository.findAllByWorkPlace_Id(workPlaceDTO.getId())
                    .stream().peek(employee -> employee.setWorkPlace(workPlace)).collect(Collectors.toList());
            if (!updatedWorkplaceEmployees.isEmpty()) {
                employeeSearchRepository.saveAll(updatedWorkplaceEmployees);
            }
        }
        WorkPlaceDTO result = mapper.toDto(workPlace);
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
