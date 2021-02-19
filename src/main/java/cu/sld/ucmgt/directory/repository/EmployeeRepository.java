package cu.sld.ucmgt.directory.repository;

import cu.sld.ucmgt.directory.domain.Employee;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    @EntityGraph(attributePaths = {
            "charge",
            "phones",
            "category",
            "district",
            "specialty",
            "workPlace",
            "profession",
            "scientificDegree",
            "teachingCategory"
    })
    Optional<Employee> findEmployeeWithAssociationsById(UUID uuid);
}
