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
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
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
    public NomenclatureDTO create(NomenclatureDTO nomenclatureDTO) {
        log.debug("Request to create Nomenclature : {}", nomenclatureDTO);
        Nomenclature nomenclature = mapper.toEntity(nomenclatureDTO);
        repository.save(nomenclature);
        return mapper.toDto(nomenclature);
    }

    public NomenclatureDTO update(NomenclatureDTO nomenclatureDTO) {
        log.debug("Request to update Nomenclature : {}", nomenclatureDTO);
        Nomenclature nomenclature = mapper.toEntity(nomenclatureDTO);
        repository.findById(nomenclatureDTO.getId()).ifPresent(fetchedNomenclature -> {
            this.updateNomenclatureByEmployeeIndex(nomenclature, fetchedNomenclature.getName());
            repository.save(nomenclature);
        });
        return mapper.toDto(nomenclature);
    }

    private void updateNomenclatureByEmployeeIndex(Nomenclature newNomenclature, String oldNomenclatureName) {
        try {
            String filedName = newNomenclature.getDiscriminator().getShortCode();
            String updateCode = "ctx._source." + filedName + "=null";
            if (newNomenclature.getActive()) {
                updateCode = "ctx._source." + filedName + "=\"" + newNomenclature.getName() + "\"";
            }
            UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest("employees")
                    .setRefresh(true)
                    .setAbortOnVersionConflict(true)
                    .setQuery(QueryBuilders.matchQuery(filedName, oldNomenclatureName))
                    .setScript(new Script(ScriptType.INLINE, "painless", updateCode, Collections.emptyMap()));
            highLevelClient.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT);

            updateByQueryRequest = new UpdateByQueryRequest("students")
                    .setRefresh(true)
                    .setAbortOnVersionConflict(true)
                    .setQuery(QueryBuilders.matchQuery(filedName, oldNomenclatureName))
                    .setScript(new Script(ScriptType.INLINE, "painless", updateCode, Collections.emptyMap()));
            highLevelClient.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT);
        } catch (ElasticsearchException | IOException e) {
            log.error(e.getMessage());
        }
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
        repository.findNomenclatureWithAssociationsById(uid).ifPresent(nomenclature -> {

            nomenclature.setActive(false);
            this.updateNomenclatureByEmployeeIndex(nomenclature, nomenclature.getName());

            if (nomenclature.getDiscriminator() == NomenclatureType.DISTRITO) {
                new HashSet<>(nomenclature.getPeopleDistrict()).forEach(nomenclature::removePeopleDistrict);
            } else if (nomenclature.getDiscriminator() == NomenclatureType.ESPECIALIDAD) {
                new HashSet<>(nomenclature.getPeopleSpecialty()).forEach(nomenclature::removePeopleSpecialty);
            } else if (nomenclature.getDiscriminator() == NomenclatureType.CATEGORIA) {
                new HashSet<>(nomenclature.getEmployeesCategory()).forEach(nomenclature::removeEmployeesCategory);
            } else if (nomenclature.getDiscriminator() == NomenclatureType.GRADO_CIENTIFICO) {
                new HashSet<>(nomenclature.getEmployeesScientificDegree()).forEach(nomenclature::removeEmployeesScientificDegree);
            } else if (nomenclature.getDiscriminator() == NomenclatureType.CATEGORIA_DOCENTE) {
                new HashSet<>(nomenclature.getEmployeesTeachingCategory()).forEach(nomenclature::removeEmployeesTeachingCategory);
            } else if (nomenclature.getDiscriminator() == NomenclatureType.CARGO) {
                nomenclature.getEmployeesCharge().forEach(nomenclature::removeEmployeesCharge);
                new HashSet<>(nomenclature.getEmployeesCharge()).forEach(nomenclature::removeEmployeesCharge);
            } else if (nomenclature.getDiscriminator() == NomenclatureType.PROFESION) {
                nomenclature.getEmployeesProfession().forEach(nomenclature::removeEmployeesProfession);
                new HashSet<>(nomenclature.getEmployeesProfession()).forEach(nomenclature::removeEmployeesProfession);
            } else if (nomenclature.getDiscriminator() == NomenclatureType.TIPO) {
                nomenclature.getStudentsKind().forEach(nomenclature::removeStudentsKind);
                new HashSet<>(nomenclature.getStudentsKind()).forEach(nomenclature::removeStudentsKind);
            } else {
                nomenclature.getStudentsStudyCenter().forEach(nomenclature::removeStudentsStudyCenter);
                new HashSet<>(nomenclature.getStudentsStudyCenter()).forEach(nomenclature::removeStudentsStudyCenter);
            }
            repository.delete(nomenclature);
        });
    }

    /**
     * Get children nomenclatures page given parentId
     * @param id district parent id
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    public Page<NomenclatureDTO> getChildrenByParentId(Pageable pageable, UUID id) {
        log.debug("Request to get a page of children district by ParentId : {}", id);
        return repository.findAllByParentDistrict_Id(pageable,id).map(mapper::toDto);
    }
}
