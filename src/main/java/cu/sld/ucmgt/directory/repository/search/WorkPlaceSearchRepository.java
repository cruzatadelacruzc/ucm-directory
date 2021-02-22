package cu.sld.ucmgt.directory.repository.search;

import cu.sld.ucmgt.directory.domain.elasticsearch.WorkPlaceIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data Elasticsearch repository for the {@link WorkPlaceIndex} entity.
 */
public interface WorkPlaceSearchRepository extends ElasticsearchRepository<WorkPlaceIndex, UUID> {
}
