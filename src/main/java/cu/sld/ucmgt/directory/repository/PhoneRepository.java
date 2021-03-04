package cu.sld.ucmgt.directory.repository;

import cu.sld.ucmgt.directory.domain.Phone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data  repository for the Phone entity.
 */
@Repository
public interface PhoneRepository extends JpaRepository<Phone, UUID> {

    void deletePhoneByNumber(Integer number);
}
