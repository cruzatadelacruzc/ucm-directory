package cu.sld.ucmgt.directory.repository.search;

import cu.sld.ucmgt.directory.domain.elasticsearch.PhoneIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data Elasticsearch repository for the {@link PhoneIndex} entity.
 */
public interface PhoneSearchRepository extends ElasticsearchRepository<PhoneIndex, UUID> {

    List<PhoneIndex> findAllByWorkPlace_Id(UUID uuid);

    List<PhoneIndex> findAllByEmployee_Id(UUID uuid);

    void deletePhoneIndexByNumber(Integer number);

    Optional<PhoneIndex> findPhoneIndexByNumber(Integer number);
}
