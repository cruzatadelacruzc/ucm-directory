package cu.sld.ucmgt.directory.domain.elasticsearch;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.UUID;


@Data
@Document(indexName = "phones", type = "phones")
public class PhoneIndex {

    @Id
    private UUID id;

    private Integer number;

    @Field(type = FieldType.Text)
    private String description;

    private EmployeeIndex employee;

    private WorkPlaceIndex workPlace;
}
