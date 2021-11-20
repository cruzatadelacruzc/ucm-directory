package cu.sld.ucmgt.directory.repository;

import cu.sld.ucmgt.directory.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> , JpaSpecificationExecutor<Student> {
}
