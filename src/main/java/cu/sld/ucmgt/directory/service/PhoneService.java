package cu.sld.ucmgt.directory.service;

import cu.sld.ucmgt.directory.domain.Employee;
import cu.sld.ucmgt.directory.domain.Phone;
import cu.sld.ucmgt.directory.domain.WorkPlace;
import cu.sld.ucmgt.directory.repository.EmployeeRepository;
import cu.sld.ucmgt.directory.repository.PhoneRepository;
import cu.sld.ucmgt.directory.repository.WorkPlaceRepository;
import cu.sld.ucmgt.directory.repository.search.PhoneSearchRepository;
import cu.sld.ucmgt.directory.service.dto.PhoneDTO;
import cu.sld.ucmgt.directory.service.mapper.PhoneMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PhoneService {

    private final PhoneMapper mapper;
    private final PhoneRepository repository;
    private final EmployeeRepository employeeRepository;
    private final PhoneSearchRepository searchRepository;
    private final WorkPlaceRepository workPlaceRepository;


    /**
     * Save a phone.
     *
     * @param phoneDTO the entity to save.
     * @return the persisted entity.
     */
    public PhoneDTO save(PhoneDTO phoneDTO) {
        log.debug("Request to save Phone : {}", phoneDTO);
        Phone phone = mapper.toEntity(phoneDTO);
        repository.save(phone);
        PhoneDTO result = mapper.toDto(phone);
        // find all employee and workplace to save in elasticsearch
        if (phone.getWorkPlace() != null) {
            workPlaceRepository.findById(phone.getWorkPlace().getId()).ifPresent(phone::setWorkPlace);
        } else {
            employeeRepository.findById(phone.getEmployee().getId()).ifPresent(phone::setEmployee);
        }
        searchRepository.save(phone);
        return result;
    }

    /**
     * Get all the phones.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<PhoneDTO> getAllPhones(Pageable pageable) {
        log.debug("Request to get all Phones");
        return repository.findAll(pageable).map(mapper::toDto);
    }

    /**
     * Get one phone by uid.
     *
     * @param uid the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<PhoneDTO> getPhone(UUID uid) {
        log.debug("Request to get Phone : {}", uid);
        return repository.findById(uid).map(mapper::toDto);
    }

    /**
     * Delete the phone by uid.
     *
     * @param uid the id of the entity.
     */
    public void deletePhone(UUID uid) {
        log.debug("Request to delete Phone : {}", uid);
        repository.deleteById(uid);
        searchRepository.deleteById(uid);
    }


}
