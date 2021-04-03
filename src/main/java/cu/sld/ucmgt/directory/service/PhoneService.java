package cu.sld.ucmgt.directory.service;

import cu.sld.ucmgt.directory.domain.Phone;
import cu.sld.ucmgt.directory.domain.elasticsearch.PhoneIndex;
import cu.sld.ucmgt.directory.repository.EmployeeRepository;
import cu.sld.ucmgt.directory.repository.PhoneRepository;
import cu.sld.ucmgt.directory.repository.WorkPlaceRepository;
import cu.sld.ucmgt.directory.repository.search.PhoneSearchRepository;
import cu.sld.ucmgt.directory.service.dto.PhoneDTO;
import cu.sld.ucmgt.directory.service.mapper.PhoneIndexMapper;
import cu.sld.ucmgt.directory.service.mapper.PhoneMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PhoneService {

    private final PhoneMapper mapper;
    private final PhoneRepository repository;
    private final PhoneIndexMapper phoneIndexMapper;
    private final RestHighLevelClient highLevelClient;
    private final EmployeeRepository employeeRepository;
    private final PhoneSearchRepository searchRepository;
    private final WorkPlaceRepository workPlaceRepository;
    private final ApplicationEventPublisher eventPublisher;


    /**
     * Save a phone.
     *
     * @param phoneDTO the entity to save.
     * @return the persisted entity.
     */
    public Phone save(PhoneDTO phoneDTO) {
        Phone phone = mapper.toEntity(phoneDTO);
        repository.save(phone);
        // find workplace to save in elasticsearch
        if (phone.getWorkPlace() != null) {
            workPlaceRepository.findById(phone.getWorkPlace().getId()).ifPresent(phone::setWorkPlace);
        } else {
            employeeRepository.findById(phone.getEmployee().getId()).ifPresent(phone::setEmployee);
        }
        return phone;
    }

    /**
     * Create a phone.
     *
     * @param phoneDTO the entity to save.
     * @return the persisted entity.
     */
    public PhoneDTO create(PhoneDTO phoneDTO) {
        log.debug("Request to create Phone : {}", phoneDTO);
        // always create enabled phone
        phoneDTO.setActive(true);
        Phone phone = save(phoneDTO);
        PhoneIndex phoneIndex = phoneIndexMapper.toIndex(phone);
        searchRepository.save(phoneIndex);
        Map<String, Object> phoneIndexMap = createPhoneIndexToPhoneIndexMap(phoneIndex);
        final SavedPhoneIndexEvent indexEvent = SavedPhoneIndexEvent.builder()
                .phoneId(null)
                .phoneIndexMap(phoneIndexMap)
                .build();
        eventPublisher.publishEvent(indexEvent);
        return mapper.toDto(phone);
    }

    /**
     * Update a phone.
     *
     * @param phoneDTO the entity to save.
     * @return the persisted entity.
     */
    public PhoneDTO update(PhoneDTO phoneDTO) {
        log.debug("Request to update Phone : {}", phoneDTO);
        Phone phone = save(phoneDTO);
        PhoneIndex phoneIndex = phoneIndexMapper.toIndex(phone);
        searchRepository.save(phoneIndex);
        Map<String, Object> phoneIndexMap = createPhoneIndexToPhoneIndexMap(phoneIndex);
        final SavedPhoneIndexEvent indexEvent = SavedPhoneIndexEvent.builder()
                .phoneIndexMap(phoneIndexMap)
                .phoneId(phoneIndex.getId())
                .build();
        eventPublisher.publishEvent(indexEvent);
        return mapper.toDto(phone);
    }

    /**
     * Create a map of {@link PhoneIndex} instance
     *
     * @param phoneIndex {@link PhoneIndex} instance
     * @return phoneMap
     */
    private Map<String, Object> createPhoneIndexToPhoneIndexMap(PhoneIndex phoneIndex){
        Map<String, Object> phoneMap = new HashMap<>();
        phoneMap.put("number", phoneIndex.getNumber());
        phoneMap.put("id", phoneIndex.getId().toString());
        phoneMap.put("description", phoneIndex.getDescription());
        phoneMap.put("workPlace", null);
        if (phoneIndex.getWorkPlace() != null){
            Map<String, Object> workPlaceMap = new HashMap<>();
            workPlaceMap.put("name", phoneIndex.getWorkPlace().getName());
            workPlaceMap.put("email", phoneIndex.getWorkPlace().getEmail());
            workPlaceMap.put("id", phoneIndex.getWorkPlace().getId().toString());
            workPlaceMap.put("description", phoneIndex.getWorkPlace().getDescription());
            phoneMap.put("workPlace", workPlaceMap);
        }
        return phoneMap;
    }

    @EventListener
    public void updateEmployeeInPhoneIndex(EmployeeService.SavedEmployeeIndexEvent  employeeIndexEvent) {
        if (employeeIndexEvent.getEmployeeId() != null) {
            try {
                String updateCode = "for (entry in params.entrySet()){ if (entry.getKey() != \"ctx\") " +
                        "{ctx._source.employee[entry.getKey()] = entry.getValue()}}";
                UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest("phones")
                  .setRefresh(true)
                  .setAbortOnVersionConflict(true)
                  .setScript(new Script(ScriptType.INLINE, "painless", updateCode, employeeIndexEvent.getParams()))
                  .setQuery(QueryBuilders.matchQuery("employee.id", employeeIndexEvent.getEmployeeId()));
                highLevelClient.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
     * Delete the phone by number.
     *
     * @param number the id of the entity.
     */
    public void deletePhone(Integer number) {
        log.debug("Request to delete Phone : {}", number);
        repository.findPhoneByNumber(number).ifPresent(phone -> {
            repository.delete(phone);
            PhoneIndex phoneIndex  = searchRepository.findPhoneIndexByNumber(number)
                    .orElseThrow(() -> new NoSuchElementException("PhoneIndex with number: " + number + " not was found"));
                searchRepository.delete(phoneIndex);
                final RemovedPhoneIndexEvent removedPhoneIndexEvent = RemovedPhoneIndexEvent.builder()
                        .removedPhoneIndex(phoneIndex)
                        .removedPhoneIndexId(phoneIndex.getId())
                        .build();
                eventPublisher.publishEvent(removedPhoneIndexEvent);
        });
    }

    /**
     * Change status for active or disable phone
     *
     * @param id          phone identifier
     * @param status true or false
     * @return true if changed status or false otherwise
     */
    public Boolean changeStatus(UUID id, Boolean status) {
        log.debug("Request to change phone status to {} by ID {}", status, id);
        Optional<Phone> phoneToUpdate = repository.findById(id);
        if (phoneToUpdate.isPresent()) {
            phoneToUpdate.get().setActive(status);
            repository.save(phoneToUpdate.get());
            if (status) {
                // PhoneIndex must to be created because when Phone was disabled, PhoneIndex was removed
                PhoneIndex phoneIndexToUpdate = phoneIndexMapper.toIndex(phoneToUpdate.get());
                searchRepository.save(phoneIndexToUpdate);
                Map<String, Object> phoneIndexMap = createPhoneIndexToPhoneIndexMap(phoneIndexToUpdate);
                final SavedPhoneIndexEvent savedPhoneIndexEvent = SavedPhoneIndexEvent.builder()
                        .phoneId(null)
                        .phoneIndexMap(phoneIndexMap)
                        .build();
                eventPublisher.publishEvent(savedPhoneIndexEvent);
            } else {
                // PhoneIndex must to be removed because Phone was disabled
                searchRepository.findPhoneIndexByNumber(phoneToUpdate.get().getNumber()).ifPresent(phoneIndexToDisable -> {
                    searchRepository.delete(phoneIndexToDisable);
                    final RemovedPhoneIndexEvent removedPhoneIndexEvent = RemovedPhoneIndexEvent.builder()
                            .removedPhoneIndex(phoneIndexToDisable)
                            .removedPhoneIndexId(phoneIndexToDisable.getId())
                            .build();
                    eventPublisher.publishEvent(removedPhoneIndexEvent);
                });
            }
            return true;
        }
        return false;
    }

    /**
     *  Class to register a saved {@link PhoneIndex} as event
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class SavedPhoneIndexEvent {
        UUID phoneId;
        Map<String,Object> phoneIndexMap;
    }

    /**
     *  Class to register a removed {@link PhoneIndex} as event
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class RemovedPhoneIndexEvent {
        UUID removedPhoneIndexId;
        PhoneIndex removedPhoneIndex;

    }

}
