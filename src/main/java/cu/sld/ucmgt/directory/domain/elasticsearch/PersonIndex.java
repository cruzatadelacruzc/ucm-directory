package cu.sld.ucmgt.directory.domain.elasticsearch;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import cu.sld.ucmgt.directory.domain.Gender;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.time.LocalDate;
import java.util.UUID;

@Data
public abstract class PersonIndex {

    protected UUID id;

    protected String ci;

    protected String name;

    protected String email;

    protected String address;

    protected String firstLastName;

    protected String secondLastName;

    @Enumerated(EnumType.STRING)
    protected Gender gender;

    protected Integer age;

    protected String race;

    protected String district;

    protected String parentDistrict;

    protected String specialty;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    protected LocalDate birthdate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PersonIndex)) return false;

        return id != null && id.equals(((PersonIndex) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public String toString() {
        return "PersonIndex{" +
                "id=" + id +
                ", ci='" + ci + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", address='" + address + '\'' +
                ", firstLastName='" + firstLastName + '\'' +
                ", secondLastName='" + secondLastName + '\'' +
                ", gender=" + gender +
                ", age=" + age +
                ", race='" + race + '\'' +
                ", district='" + district + '\'' +
                ", specialty='" + specialty + '\'' +
                ", birthdate=" + birthdate +
                ", parentDistrict=" + parentDistrict +
                '}';
    }
}
