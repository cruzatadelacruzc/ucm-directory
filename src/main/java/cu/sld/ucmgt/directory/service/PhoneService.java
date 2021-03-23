package cu.sld.ucmgt.directory.service;

import cu.sld.ucmgt.directory.domain.Phone;
import cu.sld.ucmgt.directory.domain.elasticsearch.PhoneIndex;
import cu.sld.ucmgt.directory.event.UpdatedEmployeeIndexEvent;
import cu.sld.ucmgt.directory.repository.EmployeeRepository;
import cu.sld.ucmgt.directory.repository.PhoneRepository;
import cu.sld.ucmgt.directory.repository.WorkPlaceRepository;
import cu.sld.ucmgt.directory.repository.search.PhoneSearchRepository;
import cu.sld.ucmgt.directory.service.dto.PhoneDTO;
import cu.sld.ucmgt.directory.service.mapper.PhoneIndexMapper;
import cu.sld.ucmgt.directory.service.mapper.PhoneMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
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
        Phone phone = save(phoneDTO);
        PhoneIndex phoneIndex = phoneIndexMapper.toIndex(phone);
        searchRepository.save(phoneIndex);
        return mapper.toDto(phone);
    }

    /**
     * Update a phone.
     *
     * @param phoneDTO the entity to save.
     * @return the persisted entity.
     */
    public PhoneDTO update(PhoneDTO phoneDTO) {
        log.debug("Request to create Phone : {}", phoneDTO);
        Phone phone = save(phoneDTO);
        PhoneIndex phoneIndex = phoneIndexMapper.toIndex(phone);
        searchRepository.save(phoneIndex);
        try {
            // updating the workplace belonging to phones and employees
            Map<String, Object> params = new HashMap<>();
            params.put("number", phoneIndex.getNumber());
            params.put("description", phoneIndex.getDescription());
            String updateCode = "def targets = ctx._source.phones.findAll(phone -> " +
                    "phone.id == \"" + phoneIndex.getId().toString() + "\" ); " +
                    "for (phone in targets) { for (entry in params.entrySet()) { if (entry.getKey() != \"ctx\") {" +
                    "phone[entry.getKey()] = entry.getValue() }}}";
            UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest("workplaces")
                    .setRefresh(true)
                    .setAbortOnVersionConflict(true)
                    .setScript(new Script(ScriptType.INLINE, "painless", updateCode, params));
            highLevelClient.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT);
        } catch (ElasticsearchException | IOException e) {
            e.printStackTrace();
        }
        return mapper.toDto(phone);
    }

    @EventListener
    public void updateEmployeeInPhoneIndex(UpdatedEmployeeIndexEvent employeeIndexEvent) {
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
        repository.deletePhoneByNumber(number);
        searchRepository.deletePhoneIndexByNumber(number);
        try {
            String updateCode = "ctx._source.phones.removeIf(phone -> phone.number == " + number + ")";
            UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest("workplaces")
                    .setRefresh(true)
                    .setAbortOnVersionConflict(true)
                    .setScript(new Script(ScriptType.INLINE, "painless", updateCode, Collections.emptyMap()));
            highLevelClient.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT);
        } catch (ElasticsearchException | IOException exception) {
            exception.printStackTrace();
        }
    }


}
