package cu.sld.ucmgt.directory.domain.elasticsearch;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(indexName = "employees")
public class EmployeeIndex extends PersonIndex {

    @Field(type = FieldType.Text)
    private String professionalNumber;

    @Field(type = FieldType.Text)
    protected String registerNumber;

    private WorkPlaceIndex workPlace;

    private Boolean bossWorkPlace;

    private String category;

    private String charge;

    private String profession;

    @Override
    public String toString() {
        return "EmployeeIndex{" +
                "professionalNumber='" + professionalNumber + '\'' +
                ", registerNumber='" + registerNumber + '\'' +
                ", workPlace=" + workPlace +
                ", bossWorkPlace=" + bossWorkPlace +
                ", category='" + category + '\'' +
                ", charge='" + charge + '\'' +
                ", profession='" + profession + '\'' +
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
