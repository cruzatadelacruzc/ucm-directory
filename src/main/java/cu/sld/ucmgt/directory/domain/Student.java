package cu.sld.ucmgt.directory.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.elasticsearch.annotations.Document;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.UUID;

@Data
@Entity
@Document(indexName = "students", shards = 3)
public class Student extends Person implements Serializable {

    @NotBlank
    private String classRoom;

    @Min(value = 1)
    private Integer universityYear;

    @NotBlank
    private String residence;

    @ManyToOne
    @JsonIgnoreProperties(value = "studentsCareer")
    protected Nomenclature career;

    @ManyToOne
    @JsonIgnoreProperties(value = "studentsKind")
    protected Nomenclature kind;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student)) return false;

        return id != null && id.equals(((Student) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public String toString() {
        return "Student{" +
                "classRoom='" + classRoom + '\'' +
                ", universityYear=" + universityYear +
                ", residence='" + residence + '\'' +
                ", id=" + id +
                ", CI='" + CI + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", firstLastName='" + firstLastName + '\'' +
                ", secondLastName='" + secondLastName + '\'' +
                ", gender=" + gender +
                ", age=" + age +
                ", race='" + race + '\'' +
                '}';
    }
}
