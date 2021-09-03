package cu.sld.ucmgt.directory.service;

import cu.sld.ucmgt.directory.domain.Nomenclature;
import cu.sld.ucmgt.directory.domain.NomenclatureType;
import cu.sld.ucmgt.directory.domain.Nomenclature_;
import cu.sld.ucmgt.directory.repository.NomenclatureRepository;
import cu.sld.ucmgt.directory.service.criteria.NomenclatureCriteria;
import cu.sld.ucmgt.directory.service.dto.NomenclatureDTO;
import cu.sld.ucmgt.directory.service.mapper.NomenclatureMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.JoinType;
import java.util.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NomenclatureService extends QueryService<Nomenclature> {

    private final NomenclatureMapper mapper;
    private final NomenclatureRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    enum Action {
        UPDATE,REMOVED
    }

    /**
     * Create a nomenclature.
     *
     * @param nomenclatureDTO the entity to crate.
     * @return {@link Nomenclature} the persisted entity.
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

    /**
     * Update a nomenclature and update Employee indexes if store updated nomenclature.
     * @param nomenclatureDTO the entity to crate.
     * @return {@link Nomenclature} the persisted entity.
     */
    public NomenclatureDTO update(NomenclatureDTO nomenclatureDTO) {
        log.debug("Request to update Nomenclature : {}", nomenclatureDTO);
        if (nomenclatureDTO.getParentDistrictId() != null) {
            nomenclatureDTO.setDiscriminator(NomenclatureType.DISTRITO);
        }
        Optional<Nomenclature> nomenclatureToUpdate = repository.findNomenclatureWithAssociationsById(nomenclatureDTO.getId());
        if (nomenclatureToUpdate.isPresent()) {
            nomenclatureToUpdate.get().setName(nomenclatureDTO.getName());
            nomenclatureToUpdate.get().setDescription(nomenclatureDTO.getDescription());
            this.updateNomenclatureInIndices(nomenclatureToUpdate.get(), Action.UPDATE);
            return mapper.toDto(nomenclatureToUpdate.get());
        }
        return null;
    }

    /**
     * Set nomenclature value inside indices like: employee and student;
     * @param newNomenclature updated
     */
    private void updateNomenclatureInIndices(Nomenclature newNomenclature, Action action) {
        String filedName = newNomenclature.getDiscriminator().getShortCode();
        String updateCode = "ctx._source." + filedName + "=null;";
        if (action.equals(Action.UPDATE)) {
            updateCode = "ctx._source." + filedName + "=\"" + newNomenclature.getName() + "\"";
        }
        if (!newNomenclature.isChildDistrict() && newNomenclature.getDiscriminator().equals(NomenclatureType.DISTRITO)) {
            updateCode = "ctx._source.parentDistrict=null;ctx._source.district=null;";
            if (action.equals(Action.UPDATE)) {
                updateCode = "ctx._source.district=\"" + newNomenclature.getName() + "\"";
                if (!newNomenclature.getChildrenDistrict().isEmpty()) {
                    updateCode = "ctx._source.parentDistrict=\"" + newNomenclature.getName() + "\"";
                }
            }
        }
        List<UUID> associationsIds = this.getAllAssociationIds(newNomenclature.getId());
        final SavedNomenclatureEvent savedNomenclatureEvent = SavedNomenclatureEvent.builder()
                .updateCode(updateCode)
                .updatedNomenclature(newNomenclature)
                .commonAssociationIds(associationsIds)
                .build();
        eventPublisher.publishEvent(savedNomenclatureEvent);
    }

    private List<UUID> getAllAssociationIds(UUID nomenclatureId) {
        List<UUID> associationsIds = new ArrayList<>();
        Nomenclature nomenclatureWithAllAssociations = repository.findNomenclatureWithAssociationsById(nomenclatureId)
                .orElseThrow(() -> new NoSuchElementException("Nomenclature with ID: " + nomenclatureId + " not was found"));
        if (!nomenclatureWithAllAssociations.isChildDistrict() && nomenclatureWithAllAssociations.getDiscriminator().equals(NomenclatureType.DISTRITO)) {
            Set<Nomenclature> children = nomenclatureWithAllAssociations.getChildrenDistrict();
            for (Nomenclature childNomenclature : children) {
                childNomenclature.getPeopleDistrict().forEach(person -> associationsIds.add(person.getId()));
            }
        } else {
            if (nomenclatureWithAllAssociations.getDiscriminator().equals(NomenclatureType.DISTRITO)){
                nomenclatureWithAllAssociations.getPeopleDistrict().forEach(person -> associationsIds.add(person.getId()));
            }
            if (nomenclatureWithAllAssociations.getDiscriminator().equals(NomenclatureType.ESPECIALIDAD)){
                nomenclatureWithAllAssociations.getPeopleDistrict().forEach(person -> associationsIds.add(person.getId()));
            }
        }
        return associationsIds;
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

    @Transactional(readOnly = true)
    public Optional<Nomenclature> getNomenclatureByIdAndCheckParentDiscriminatorWithUniqueNameAndUniqueDiscriminator(String name,
                                                                                          NomenclatureType discriminator,
                                                                                          UUID id, boolean isParentDistrict) {
        log.debug("Request to get a Nomenclature by ID {}, Name {} and Discriminator {}", id, name, discriminator);
       return repository.findNomenclatureWithUniqueNameAndUniqueDiscriminator(name, discriminator, id, isParentDistrict);
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
            this.updateNomenclatureInIndices(nomenclature, Action.REMOVED);
           if (!nomenclature.getChildrenDistrict().isEmpty()) {
               for (Nomenclature child : nomenclature.getChildrenDistrict()) {
                   Nomenclature nomenclatureChild = repository.findNomenclatureWithAssociationsById(child.getId())
                           .orElseThrow(() -> new NoSuchElementException("Nomenclature with ID: " + child.getId() + " not was found"));
                   this.removeNomenclatureAssociations(nomenclatureChild);
               }
           }else {
               this.removeNomenclatureAssociations(nomenclature);
           }
            repository.delete(nomenclature);
        });
    }

    private void removeNomenclatureAssociations(Nomenclature nomenclature){
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

    /**
     * Get nomenclatures page given status and discriminator
     *
     * @param discriminator nomenclature discriminator
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    public Page<NomenclatureDTO> getAllByStatusAndDiscriminator(Pageable pageable,NomenclatureType discriminator) {
        log.debug("Request to get a page of Nomenclature by discriminator {}", discriminator);
        return repository.findAllByDiscriminator(pageable, discriminator).map(mapper::toDto);
    }

    /**
     * Return a {@link List} of {@link NomenclatureDTO} which matches the criteria from the database.
     * @param operator_union Logical operator to join expression: AND - OR
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching entities.
     */
    public Page<NomenclatureDTO> findByCriteria(String operator_union, NomenclatureCriteria criteria, Pageable page) {
        log.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Nomenclature> specification = createSpecification(operator_union, criteria);
        return repository.findAll(specification, page).map(mapper::toDto);
    }

    /**
     * Function to convert {@link NomenclatureCriteria} to a {@link Specification}
     * @param operator_union Logical operator to join expression: AND - OR
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    private Specification<Nomenclature> createSpecification(String operator_union, NomenclatureCriteria criteria) {
        Specification<Nomenclature> specification = Specification.where(null);
        if (criteria != null) {
            if (operator_union.equalsIgnoreCase("AND")) {
                if (criteria.getId() != null) {
                    specification = specification.and(buildSpecification(criteria.getId(), Nomenclature_.id));
                }
                if (criteria.getName() != null) {
                    specification = specification.and(buildStringSpecification(criteria.getName(), Nomenclature_.name));
                }
                if (criteria.getDescription() != null) {
                    specification = specification.and(buildStringSpecification(criteria.getDescription(), Nomenclature_.description));
                }
                if (criteria.getDiscriminator() != null) {
                    specification = specification.and(buildAsStringSpecification(criteria.getDiscriminator(), Nomenclature_.discriminator));
                }
                if (criteria.getParentDistrictName() != null) {
                    specification = specification.and(buildSpecification(criteria.getParentDistrictName(),
                            root -> root.join(Nomenclature_.parentDistrict, JoinType.LEFT).get(Nomenclature_.name)));
                }
            } else {
                if (criteria.getId() != null) {
                    specification = specification.or(buildSpecification(criteria.getId(), Nomenclature_.id));
                }
                if (criteria.getName() != null) {
                    specification = specification.or(buildStringSpecification(criteria.getName(), Nomenclature_.name));
                }
                if (criteria.getDescription() != null) {
                    specification = specification.or(buildStringSpecification(criteria.getDescription(), Nomenclature_.description));
                }
                if (criteria.getDiscriminator() != null) {
                    specification = specification.or(buildAsStringSpecification(criteria.getDiscriminator(), Nomenclature_.discriminator));
                }
                if (criteria.getParentDistrictName() != null) {
                    specification = specification.or(buildSpecification(criteria.getParentDistrictName(),
                            root -> root.join(Nomenclature_.parentDistrict, JoinType.LEFT).get(Nomenclature_.name)));
                }
            }
        }
        return specification;
    }

    /**
     * Class to register a removed {@link Nomenclature} as event
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class SavedNomenclatureEvent {
        private String updateCode;
        private List<UUID> commonAssociationIds;
        private Nomenclature updatedNomenclature;
    }
}
