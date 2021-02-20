package cu.sld.ucmgt.directory.repository;

import cu.sld.ucmgt.directory.domain.Nomenclature;
import cu.sld.ucmgt.directory.domain.NomenclatureType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NomenclatureRepository extends JpaRepository<Nomenclature, UUID> {

    Optional<Nomenclature> findNomenclatureByNameAndDiscriminator(String name, NomenclatureType discriminator);

    @EntityGraph(attributePaths = {
            "employeesCharge",
            "peopleDistrict",
            "peopleSpecialty",
            "employeesCategory",
            "employeesScientificDegree",
            "employeesTeachingCategory",
            "employeesProfession",
            "studentsKind",
            "studentsStudyCenter"
    })
    Optional<Nomenclature> findNomenclatureWithAssociationsById(UUID uuid);
}
