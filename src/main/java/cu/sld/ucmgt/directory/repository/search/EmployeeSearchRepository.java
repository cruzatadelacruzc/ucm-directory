package cu.sld.ucmgt.directory.repository.search;

import cu.sld.ucmgt.directory.domain.Employee;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;
import java.util.UUID;
/**
 * Spring Data Elasticsearch repository for the {@link Employee} entity.
 */
public interface EmployeeSearchRepository extends ElasticsearchRepository<Employee, UUID> {

    List<Employee> findAllByWorkPlace_Id(UUID id);
}
