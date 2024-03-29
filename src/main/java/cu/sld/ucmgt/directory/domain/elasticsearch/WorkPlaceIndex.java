package cu.sld.ucmgt.directory.domain.elasticsearch;

import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Document(indexName = "workplaces")
public class WorkPlaceIndex {

    private UUID id;

    private String name;

    private String email;

    private String avatarUrl;

    @Field(type = FieldType.Text)
    private String description;

    private Set<EmployeeIndex> employees = new HashSet<>();

    private Set<PhoneIndex> phones = new HashSet<>();
}
