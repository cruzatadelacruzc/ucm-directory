package cu.sld.ucmgt.directory.service;

import cu.sld.ucmgt.directory.domain.Nomenclature;
import cu.sld.ucmgt.directory.domain.Nomenclature_;
import cu.sld.ucmgt.directory.repository.NomenclatureRepository;
import cu.sld.ucmgt.directory.service.criteria.NomenclatureCriteria;
import cu.sld.ucmgt.directory.service.dto.NomenclatureDTO;
import cu.sld.ucmgt.directory.service.mapper.NomenclatureMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for executing complex queries for {@link Nomenclature} entities in the database.
 * The main input is a {@link NomenclatureCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link List} of {@link NomenclatureDTO} or a {@link Page} of {@link NomenclatureDTO} which fulfills the criteria.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NomenclatureQueryService extends QueryService<Nomenclature> {

    private final NomenclatureMapper mapper;
    private final NomenclatureRepository repository;

    /**
     * Return a {@link List} of {@link NomenclatureDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching entities.
     */
    public Page<NomenclatureDTO> findByCriteria(NomenclatureCriteria criteria, Pageable page) {
        log.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Nomenclature> specification = createSpecification(criteria);
        return repository.findAll(specification, page).map(mapper::toDto);
    }

    /**
     * Function to convert {@link NomenclatureCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    private Specification<Nomenclature> createSpecification(NomenclatureCriteria criteria) {
        Specification<Nomenclature> specification = Specification.where(null);
        if (criteria != null) {
            if (criteria.getId() != null) {
                specification =  specification.and(buildSpecification(criteria.getId(), Nomenclature_.id));
            }
        }
        return specification;
    }
}
