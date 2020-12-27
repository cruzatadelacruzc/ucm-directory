package cu.sld.ucmgt.directory.repository;

import cu.sld.ucmgt.directory.domain.WorkPlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WorkPlaceRepository extends JpaRepository<WorkPlace, UUID> {
}
