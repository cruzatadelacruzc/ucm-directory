package cu.sld.ucmgt.directory.repository.search;

import cu.sld.ucmgt.directory.domain.elasticsearch.StudentIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.UUID;

/**
 * Spring Data Elasticsearch repository for the {@link StudentIndex} entity.
 */
public interface StudentSearchRepository extends ElasticsearchRepository<StudentIndex, UUID> {
}
