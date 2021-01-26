package cu.sld.ucmgt.directory.service;

import cu.sld.ucmgt.directory.domain.Nomenclature;
import cu.sld.ucmgt.directory.domain.NomenclatureType;
import cu.sld.ucmgt.directory.repository.NomenclatureRepository;
import cu.sld.ucmgt.directory.service.dto.NomenclatureDTO;
import cu.sld.ucmgt.directory.service.mapper.NomenclatureMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NomenclatureService {

    private final NomenclatureMapper mapper;
    private final NomenclatureRepository repository;
    private final RestHighLevelClient highLevelClient;

    /**
     * Save a nomenclature and update Employee indexes if store updated nomenclature.
     *
     * @param nomenclatureDTO the entity to save.
     * @return the persisted entity.
     */
    public NomenclatureDTO save(NomenclatureDTO nomenclatureDTO) {
        log.debug("Request to save Nomenclature : {}", nomenclatureDTO);
        Nomenclature nomenclature = mapper.toEntity(nomenclatureDTO);
        repository.save(nomenclature);
        if (nomenclatureDTO.getId() != null) {
            try {
                String filedName = nomenclature.getDiscriminator().getShortCode();
                Map<String, Object> params = new HashMap<>();
                String updateCode = "ctx._source." + filedName + "= null";
                if (nomenclatureDTO.getActive()) {
                    params.put("name", nomenclature.getName());
                    params.put("description", nomenclature.getDescription());
                    params.put("discriminator", nomenclature.getDiscriminator().toString());
                    updateCode = "for (entry in params.entrySet()){if (entry.getKey() != \"ctx\") " +
                            "{ctx._source." + filedName + "[entry.getKey()] = entry.getValue()}}";
                }

                UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest("employees");
                updateByQueryRequest.setAbortOnVersionConflict(true);
                updateByQueryRequest.setQuery(QueryBuilders.matchQuery(filedName + ".id", nomenclatureDTO.getId().toString()));
                updateByQueryRequest.setScript(new Script(ScriptType.INLINE, "painless", updateCode, params));
                BulkByScrollResponse bulkResponse = highLevelClient.updateByQuery(
                        updateByQueryRequest, RequestOptions.DEFAULT);
                log.debug("Amount affected Employee indexes: {}", bulkResponse.getUpdated());
            } catch (ElasticsearchException | IOException e) {
                log.error(e.getMessage());
            }
        }
        return mapper.toDto(nomenclature);
    }

    /**
     * Returns a Nomenclature given a name and discriminator
     *
     * @param name          Nomenclature name
     * @param discriminator NomenclatureType
     * @return {@link Nomenclature} instance if founded
     */
    @Transactional(readOnly = true)
    public Optional<Nomenclature> findNomenclatureByNameAndDiscriminator(String name, NomenclatureType discriminator) {
        log.debug("Request to find a Nomenclature by Name : {}", name);
        return repository.findNomenclatureByNameAndDiscriminator(name, discriminator);
    }

    /**
     * Get one nomenclature by uid.
     *
     * @param uid the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<NomenclatureDTO> getNomenclatures(UUID uid) {
        log.debug("Request to get Nomenclature : {}", uid);
        return repository.findById(uid).map(mapper::toDto);
    }

    /**
     * Get all the nomenclatures.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<NomenclatureDTO> getAllNomenclatures(Pageable pageable) {
        log.debug("Request to get all Nomenclatures");
        return repository.findAll(pageable).map(mapper::toDto);
    }

    /**
     * Delete the nomenclature by uid.
     *
     * @param uid the id of the entity.
     */
    public void deleteNomenclature(UUID uid) {
        log.debug("Request to delete Nomenclature : {}", uid);
        repository.deleteById(uid);
    }
}
