package cu.sld.ucmgt.directory.service;

import cu.sld.ucmgt.directory.domain.Nomenclature;
import cu.sld.ucmgt.directory.domain.NomenclatureType;
import cu.sld.ucmgt.directory.domain.Nomenclature_;
import cu.sld.ucmgt.directory.repository.NomenclatureRepository;
import cu.sld.ucmgt.directory.service.criteria.NomenclatureCriteria;
import cu.sld.ucmgt.directory.service.dto.NomenclatureDTO;
import cu.sld.ucmgt.directory.service.filter.StringFilter;
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
     * Create nomenclature
     * @param nomenclatureDTO data request
     * @return created nomenclature
     */
    public NomenclatureDTO create(NomenclatureDTO nomenclatureDTO) {
        Nomenclature nomenclature = mapper.toEntity(nomenclatureDTO);
        repository.save(nomenclature);
        return mapper.toDto(nomenclature);
    }

    /**
     * Update nomenclature, only name and description
     * @param nomenclatureDTO data request
     * @return updated nomenclature
     */
    public NomenclatureDTO update(NomenclatureDTO nomenclatureDTO) {
        Nomenclature nomenclatureWithAllAssociations = repository.findNomenclatureWithAssociationsById(nomenclatureDTO.getId())
                .orElseThrow(() -> new NoSuchElementException("Nomenclature with ID: " + nomenclatureDTO.getId() + " not was found"));
        nomenclatureWithAllAssociations.setName(nomenclatureDTO.getName());
        nomenclatureWithAllAssociations.setDescription(nomenclatureDTO.getDescription());
        this.updateNomenclatureInIndices(nomenclatureWithAllAssociations, Action.UPDATE);
        return mapper.toDto(nomenclatureWithAllAssociations);
    }

    /**
     * Check if nomenclature exists
     * @param nomenclatureId of nomenclature
     * @return boolean
     */
    public Boolean exists(UUID nomenclatureId) {
        return repository.existsById(nomenclatureId);
    }


    /**
     * Update nomenclature value inside of indices(employee and student). Only district and specialty associations
     * @param nomenclatureWithAllAssociations wit all associations
     * @param action {@literal Action.UPDATE} or {@literal Action.REMOVED}
     */
    private void updateNomenclatureInIndices(Nomenclature nomenclatureWithAllAssociations, Action action) {

        String filedName = nomenclatureWithAllAssociations.getDiscriminator().getShortCode().toLowerCase();
        String updateCode = "ctx._source." + filedName + "=null;";
        if (action.equals(Action.UPDATE)) {
            updateCode = "ctx._source." + filedName + "=\"" + nomenclatureWithAllAssociations.getName() + "\";";
        }
        List<UUID> associationsIds = new ArrayList<>();
        if (nomenclatureWithAllAssociations.getDiscriminator().equals(NomenclatureType.DISTRITO)) {
            nomenclatureWithAllAssociations.getPeopleDistrict().forEach(person -> associationsIds.add(person.getId()));
        }
        if (nomenclatureWithAllAssociations.getDiscriminator().equals(NomenclatureType.ESPECIALIDAD)) {
            nomenclatureWithAllAssociations.getPeopleSpecialty().forEach(person -> associationsIds.add(person.getId()));
        }

        final SavedNomenclatureEvent savedNomenclatureEvent = SavedNomenclatureEvent.builder()
                .updateCode(updateCode)
                .updatedNomenclature(nomenclatureWithAllAssociations)
                .commonAssociationIds(associationsIds)
                .build();
        eventPublisher.publishEvent(savedNomenclatureEvent);
    }

    @Transactional(readOnly = true)
    public Optional<Nomenclature> getNomenclatureByIdAndNameAndDiscriminator(UUID id, String name, NomenclatureType discriminator) {
       return repository.findNomenclatureWithUniqueNameAndUniqueDiscriminator(id, name, discriminator);
    }

    /**
     * Get one nomenclature by uid.
     *
     * @param uid the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<NomenclatureDTO> getNomenclature(UUID uid) {
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
        Nomenclature nomenclatureWithAllAssociations = repository.findNomenclatureWithAssociationsById(uid)
                .orElseThrow(() -> new NoSuchElementException("Nomenclature with ID: " + uid + " not was found"));
        this.updateNomenclatureInIndices(nomenclatureWithAllAssociations, Action.REMOVED);
        this.removeNomenclatureAssociations(uid);
        repository.deleteById(uid);
    }

    private void removeNomenclatureAssociations(UUID nomenclatureId) {
        Nomenclature nomenclature = repository.findNomenclatureWithAssociationsById(nomenclatureId)
                .orElseThrow(() -> new NoSuchElementException("Nomenclature with ID: " + nomenclatureId + " not was found"));
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
     * Get nomenclatures page given status and discriminator
     *
     * @param discriminator nomenclature discriminator
     * @param pageable      the pagination information.
     * @return the list of entities.
     */
    public Page<NomenclatureDTO> getAllByStatusAndDiscriminator(String operator_union, NomenclatureCriteria criteria,
                                                                NomenclatureType discriminator, Pageable pageable) {

        StringFilter stringFilter = new StringFilter();
        stringFilter.setEquals(discriminator.name());
        Specification<Nomenclature> specifications = createSpecification(operator_union, criteria);
        specifications = specifications.and(buildAsStringSpecification(stringFilter, Nomenclature_.discriminator));
        return repository.findAll(specifications, pageable).map(mapper::toDto);
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
