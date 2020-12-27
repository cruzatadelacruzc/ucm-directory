package cu.sld.ucmgt.directory.repository.search;

import cu.sld.ucmgt.directory.domain.Student;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.UUID;
/**
 * Spring Data Elasticsearch repository for the {@link Student} entity.
 */
public interface StudentSearchRepository extends ElasticsearchRepository<Student, UUID> {
}
