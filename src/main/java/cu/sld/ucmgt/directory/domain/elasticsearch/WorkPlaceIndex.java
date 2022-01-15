package cu.sld.ucmgt.directory.domain.elasticsearch;

import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Document;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Document(indexName = "workplaces", type = "workplaces")
public class WorkPlaceIndex {

    private UUID id;

    private String name;

    private String email;

    private String avatarUrl;

    private String description;

    private Set<EmployeeIndex> employees = new HashSet<>();

    private Set<PhoneIndex> phones = new HashSet<>();
}
