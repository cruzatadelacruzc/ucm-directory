package cu.sld.ucmgt.directory.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Document;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Document(indexName = "employees", shards = 3)
public class Employee extends Person implements Serializable {

    @NotNull
    private ZonedDateTime startDate;

    private ZonedDateTime endDate;

    @Min(value = 0)
    private Integer graduateYears;

    private Boolean isGraduatedBySector;

    @Min(value = 0)
    private Integer serviceYears;

    @NotBlank
    protected String registerNumber;

    private Boolean bossWorkPlace;

    private String professionalNumber;

    @ManyToOne
    @JsonIgnoreProperties(value = "employees")
    private WorkPlace workPlace;

    @ManyToOne
    @JsonIgnoreProperties(value = "employeesSpecialty")
    private Nomenclature specialty;

    @ManyToOne
    @JsonIgnoreProperties(value = "employeesCategory")
    private Nomenclature category;

    @ManyToOne
    @JsonIgnoreProperties(value = "employeesScientificDegree")
    private Nomenclature scientificDegree;

    @ManyToOne
    @JsonIgnoreProperties(value = "employeesTeachingCategory")
    private Nomenclature teachingCategory;

    @ManyToOne
    @JsonIgnoreProperties(value = "employeesCharge")
    private Nomenclature charge;

    @ManyToOne
    @JsonIgnoreProperties(value = "employeesProfession")
    protected Nomenclature profession;

    @OneToMany(mappedBy = "employee")
    private Set<Phone> phones = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Employee)) return false;

        return id != null && id.equals(((Employee) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public String toString() {
        return "Employee{" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                ", graduateYears=" + graduateYears +
                ", isGraduatedBySector=" + isGraduatedBySector +
                ", serviceYears=" + serviceYears +
                ", bossWorkPlace=" + bossWorkPlace +
                ", workPlace=" + workPlace +
                ", specialty=" + specialty +
                ", category=" + category +
                ", scientificDegree=" + scientificDegree +
                ", teachingCategory=" + teachingCategory +
                ", charge=" + charge +
                ", id=" + id +
                ", CI='" + CI + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", firstLastName='" + firstLastName + '\'' +
                ", secondLastName='" + secondLastName + '\'' +
                ", gender=" + gender +
                ", age=" + age +
                ", race='" + race + '\'' +
                ", registerNumber='" + registerNumber + '\'' +
                ", professionalNumber='" + professionalNumber + '\'' +
                '}';
    }
}
