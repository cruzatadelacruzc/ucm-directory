package cu.sld.ucmgt.directory.repository.search;

import cu.sld.ucmgt.directory.domain.elasticsearch.EmployeeIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;
import java.util.UUID;
/**
 * Spring Data Elasticsearch repository for the {@link EmployeeIndex} entity.
 */
public interface EmployeeSearchRepository extends ElasticsearchRepository<EmployeeIndex, UUID> {

    List<EmployeeIndex> findAllByWorkPlace_Id(UUID uuid);
}
