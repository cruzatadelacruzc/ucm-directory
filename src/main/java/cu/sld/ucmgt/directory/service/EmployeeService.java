package cu.sld.ucmgt.directory.service;

import cu.sld.ucmgt.directory.domain.Employee;
import cu.sld.ucmgt.directory.domain.Nomenclature;
import cu.sld.ucmgt.directory.domain.Phone;
import cu.sld.ucmgt.directory.repository.EmployeeRepository;
import cu.sld.ucmgt.directory.repository.NomenclatureRepository;
import cu.sld.ucmgt.directory.repository.WorkPlaceRepository;
import cu.sld.ucmgt.directory.repository.search.EmployeeSearchRepository;
import cu.sld.ucmgt.directory.repository.search.PhoneSearchRepository;
import cu.sld.ucmgt.directory.service.dto.EmployeeDTO;
import cu.sld.ucmgt.directory.service.mapper.EmployeeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeMapper mapper;
    private final EmployeeRepository repository;
    private final WorkPlaceRepository workPlaceRepository;
    private final EmployeeSearchRepository searchRepository;
    private final PhoneSearchRepository phoneSearchRepository;
    private final NomenclatureRepository nomenclatureRepository;

    /**
     * Save a employee.
     *
     * @param employeeDTO the entity to save.
     * @return the persisted entity.
     */
    public EmployeeDTO save(EmployeeDTO employeeDTO) {
        log.debug("Request to save Employee : {}", employeeDTO);
        Employee employee = mapper.toEntity(employeeDTO);
        repository.save(employee);
        EmployeeDTO result = mapper.toDto(employee);
        // find all nomenclatures and workplace to save in elasticsearch
        if (employee.getCategory() != null) {
            nomenclatureRepository.findById(employee.getCategory().getId()).ifPresent(employee::setCategory);
        }
        if (employee.getCharge() != null) {
            nomenclatureRepository.findById(employee.getCharge().getId()).ifPresent(employee::setCharge);
        }
        if (employee.getScientificDegree() != null) {
            nomenclatureRepository.findById(employee.getScientificDegree().getId()).ifPresent(employee::setScientificDegree);
        }
        if (employee.getSpecialty() != null) {
            nomenclatureRepository.findById(employee.getSpecialty().getId()).ifPresent(employee::setSpecialty);
        }
        if (employee.getTeachingCategory() != null) {
            nomenclatureRepository.findById(employee.getTeachingCategory().getId()).ifPresent(employee::setTeachingCategory);
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
        // updating the employee belonging to phones
        if (employeeDTO.getId() != null) {
            List<Phone> updatedEmployeePhones = phoneSearchRepository.findAllByEmployee_Id(employeeDTO.getId()).stream()
                    .peek(phone -> phone.setEmployee(employee)).collect(Collectors.toList());
            if (!updatedEmployeePhones.isEmpty()){
                phoneSearchRepository.saveAll(updatedEmployeePhones);
            }
        }
        searchRepository.save(employee);
        return result;
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
}
