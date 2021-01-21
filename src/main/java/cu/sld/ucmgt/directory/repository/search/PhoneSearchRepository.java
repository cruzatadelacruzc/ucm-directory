package cu.sld.ucmgt.directory.repository.search;

import cu.sld.ucmgt.directory.domain.Phone;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data Elasticsearch repository for the {@link Phone} entity.
 */
public interface PhoneSearchRepository extends ElasticsearchRepository<Phone, UUID> {

    List<Phone> findAllByWorkPlace_Id(UUID uuid);
}
