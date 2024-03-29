package cu.sld.ucmgt.directory.domain.elasticsearch;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(indexName = "students")
public class StudentIndex extends PersonIndex {

    @Field(type = FieldType.Text)
    private String classRoom;

    private Integer universityYear;

    private String residence;

    @Override
    public String toString() {
        return "StudentIndex{" +
                "classRoom='" + classRoom + '\'' +
                ", universityYear=" + universityYear +
                ", residence='" + residence + '\'' +
                ", id=" + id +
                ", ci='" + ci + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", address='" + address + '\'' +
                ", firstLastName='" + firstLastName + '\'' +
                ", secondLastName='" + secondLastName + '\'' +
                ", gender=" + gender +
                ", race='" + race + '\'' +
                ", district='" + district + '\'' +
                ", specialty='" + specialty + '\'' +
                ", birthdate=" + birthdate +
                '}';
    }
}
