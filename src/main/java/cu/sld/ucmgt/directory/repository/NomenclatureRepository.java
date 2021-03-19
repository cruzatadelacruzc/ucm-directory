package cu.sld.ucmgt.directory.repository;

import cu.sld.ucmgt.directory.domain.Nomenclature;
import cu.sld.ucmgt.directory.domain.NomenclatureType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NomenclatureRepository extends JpaRepository<Nomenclature, UUID>{

    Optional<Nomenclature> findNomenclatureByNameIgnoreCaseAndDiscriminatorAndParentDistrictNotNull(String name, NomenclatureType discriminator);

    Optional<Nomenclature> findNomenclatureByNameIgnoreCaseAndDiscriminatorAndParentDistrictNull(String name, NomenclatureType discriminator);

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

    Page<Nomenclature> findAllByParentDistrict_Id(Pageable pageable, UUID id);

    Page<Nomenclature> findAllByActiveAndDiscriminator(Pageable pageable, Boolean active, NomenclatureType discriminator);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Nomenclature n SET n.active = :status WHERE n.id = :id OR n.parentDistrict.id = :id")
    int updateByIdOrParentDistrict_Id(@Param("status") boolean status, @Param("id") UUID id);
}
