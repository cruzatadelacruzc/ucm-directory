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
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
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
        if (nomenclatureDTO.getParentDistrictId() != null) {
            nomenclatureDTO.setDiscriminator(NomenclatureType.DISTRITO);
        }
        Nomenclature nomenclature = mapper.toEntity(nomenclatureDTO);
        repository.save(nomenclature);
        return mapper.toDto(nomenclature);
    }

    public NomenclatureDTO update(NomenclatureDTO nomenclatureDTO) {
        log.debug("Request to update Nomenclature : {}", nomenclatureDTO);
        if (nomenclatureDTO.getParentDistrictId() != null) {
            nomenclatureDTO.setDiscriminator(NomenclatureType.DISTRITO);
        }

        Optional<Nomenclature> nomenclatureToUpdate = repository.findById(nomenclatureDTO.getId());
        if (nomenclatureToUpdate.isPresent()) {
            String oldNomenclatureName = nomenclatureToUpdate.get().getName();
            nomenclatureToUpdate.get().setName(nomenclatureDTO.getName());
            nomenclatureToUpdate.get().setActive(nomenclatureDTO.getActive());
            nomenclatureToUpdate.get().setDescription(nomenclatureDTO.getDescription());
            this.updateIndices(nomenclatureToUpdate.get(), oldNomenclatureName);
            if (!nomenclatureToUpdate.get().isChildDistrict() && !nomenclatureToUpdate.get().getActive() &&
                    nomenclatureToUpdate.get().getDiscriminator().equals(NomenclatureType.DISTRITO)){
                repository.updateByIdOrParentDistrict_Id(false, nomenclatureDTO.getId());
            }
            return mapper.toDto(nomenclatureToUpdate.get());
        }
        return null;
    }

    private void updateIndices(Nomenclature newNomenclature, String oldNomenclatureName) {
        String filedName = newNomenclature.getDiscriminator().getShortCode();
        String updateCode = "ctx._source." + filedName + "=null;";
        if (newNomenclature.getActive()) {
            updateCode = "ctx._source." + filedName + "=\"" + newNomenclature.getName() + "\"";
        }
        if (!newNomenclature.isChildDistrict() &&
                newNomenclature.getDiscriminator().equals(NomenclatureType.DISTRITO)){
            updateCode = "ctx._source.district=null;ctx._source.parentDistrict=null;";
            if (newNomenclature.getActive()){
                updateCode = "ctx._source." + filedName + "=\"" + newNomenclature.getName() + "\"";
            }
        }
        BoolQueryBuilder query = new BoolQueryBuilder()
                .should(QueryBuilders.matchQuery(filedName, oldNomenclatureName))
                .should(QueryBuilders.matchQuery("parentDistrict", oldNomenclatureName));
        updateNomenclatureInEmployeeIndex(query, updateCode);
        updateNomenclatureInStudentIndex(query, updateCode);
    }

    private void updateNomenclatureInStudentIndex(QueryBuilder query, String updateCode) {
        try {
            UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest("students")
                    .setRefresh(true)
                    .setAbortOnVersionConflict(true)
                    .setQuery(query)
                    .setScript(new Script(ScriptType.INLINE, "painless", updateCode, Collections.emptyMap()));
            highLevelClient.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT);
        } catch (ElasticsearchException | IOException e) {
            e.printStackTrace();
        }
    }

    private void updateNomenclatureInEmployeeIndex(QueryBuilder query, String updateCode) {
        try {
            UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest("employees")
                    .setRefresh(true)
                    .setAbortOnVersionConflict(true)
                    .setQuery(query)
                    .setScript(new Script(ScriptType.INLINE, "painless", updateCode, Collections.emptyMap()));
            highLevelClient.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT);
        } catch (ElasticsearchException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns a Nomenclature child of parent district given a name and discriminator
     *
     * @param name          Nomenclature name
     * @param discriminator NomenclatureType
     * @return {@link Nomenclature} instance if founded
     */
    @Transactional(readOnly = true)
    public Optional<Nomenclature> findNomenclatureChildByNameAndDiscriminator(String name, NomenclatureType discriminator) {
        log.debug("Request to find a Nomenclature child of parent district by Name : {}", name);
        return repository.findNomenclatureByNameIgnoreCaseAndDiscriminatorAndParentDistrictNotNull(name, discriminator);
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
        log.debug("Request to find a Nomenclature by Name {} and Discriminator {}", name, discriminator);
        return repository.findNomenclatureByNameIgnoreCaseAndDiscriminatorAndParentDistrictNull(name, discriminator);
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
            this.updateIndices(nomenclature, nomenclature.getName());

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
     *
     * @param id       district parent id
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    public Page<NomenclatureDTO> getChildrenByParentId(Pageable pageable, UUID id) {
        log.debug("Request to get a page of children district by ParentId : {}", id);
        return repository.findAllByParentDistrict_Id(pageable, id).map(mapper::toDto);
    }
}
