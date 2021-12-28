package cu.sld.ucmgt.directory.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Objects;

@Data
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Student extends Person implements Serializable {

    @NotBlank
    private String classRoom;

    @Min(value = 1)
    private Integer universityYear;

    @NotBlank
    private String residence;

    @ManyToOne
    @JsonIgnoreProperties(value = "studentsKind", allowSetters = true)
    private Nomenclature kind;

    @ManyToOne
    @JsonIgnoreProperties(value = "studentsStudyCenter", allowSetters = true)
    private Nomenclature studyCenter;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student)) return false;

        return id != null && id.equals(((Student) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Student{" +
                "classRoom='" + classRoom + '\'' +
                ", universityYear=" + universityYear +
                ", residence='" + residence + '\'' +
                ", kind=" + kind +
                ", studyCenter=" + studyCenter +
                "} " + super.toString();
    }
}
