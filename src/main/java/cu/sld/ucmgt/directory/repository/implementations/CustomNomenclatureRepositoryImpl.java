package cu.sld.ucmgt.directory.repository.implementations;

import cu.sld.ucmgt.directory.domain.Nomenclature;
import cu.sld.ucmgt.directory.domain.NomenclatureType;
import cu.sld.ucmgt.directory.repository.CustomNomenclatureRepository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CustomNomenclatureRepositoryImpl implements CustomNomenclatureRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<Nomenclature> findNomenclatureWithUniqueNameAndUniqueDiscriminator(UUID id, String name,
                                                                                       NomenclatureType discriminator) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Nomenclature> query = cb.createQuery(Nomenclature.class);
        Root<Nomenclature> root = query.from(Nomenclature.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("name"), name));
        predicates.add(cb.equal(root.get("discriminator"), discriminator));
        if (id != null) {
            predicates.add(cb.notEqual(root.get("id"), id));
        }
        query.select(root).where(predicates.toArray(new Predicate[0]));
        return em.createQuery(query).getResultStream().findFirst();
    }
}
