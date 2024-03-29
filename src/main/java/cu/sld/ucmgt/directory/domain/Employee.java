package cu.sld.ucmgt.directory.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Data;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Data
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Employee extends Person implements Serializable {

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime endDate;

    @Min(value = 0)
    private Integer graduateYears;

    private Boolean isGraduatedBySector;

    @Min(value = 0)
    private Integer serviceYears;

    @Min(value = 0)
    private Integer salary;

    @NotBlank
    private String registerNumber;

    private Boolean bossWorkPlace;

    private String professionalNumber;

    @ManyToOne
    @JsonIgnoreProperties(value = "employees", allowSetters = true)
    private WorkPlace workPlace;

    @ManyToOne
    @JsonIgnoreProperties(value = "employeesCategory", allowSetters = true)
    private Nomenclature category;

    @ManyToOne
    @JsonIgnoreProperties(value = "employeesScientificDegree", allowSetters = true)
    private Nomenclature scientificDegree;

    @ManyToOne
    @JsonIgnoreProperties(value = "employeesTeachingCategory", allowSetters = true)
    private Nomenclature teachingCategory;

    @ManyToOne
    @JsonIgnoreProperties(value = "employeesCharge", allowSetters = true)
    private Nomenclature charge;

    @ManyToOne
    @JsonIgnoreProperties(value = "employeesProfession", allowSetters = true)
    private Nomenclature profession;

    @OneToMany(mappedBy = "employee", orphanRemoval = true, cascade = CascadeType.PERSIST)
    @JsonIgnoreProperties(value = { "employee", "workPlace" }, allowSetters = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Phone> phones = new HashSet<>();

    public void removePhone(Phone phone) {
        this.phones.remove(phone);
        phone.setEmployee(null);
    }

    public void addPhone(Phone phone) {
        this.phones.add(phone);
        phone.setEmployee(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Employee)) return false;

        return id != null && id.equals(((Employee) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Employee{" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                ", graduateYears=" + graduateYears +
                ", isGraduatedBySector=" + isGraduatedBySector +
                ", serviceYears=" + serviceYears +
                ", registerNumber='" + registerNumber + '\'' +
                ", bossWorkPlace=" + bossWorkPlace +
                ", professionalNumber='" + professionalNumber + '\'' +
                ", salary='" + salary + '\'' +
                ", workPlace=" + workPlace +
                ", category=" + category +
                ", scientificDegree=" + scientificDegree +
                ", teachingCategory=" + teachingCategory +
                ", charge=" + charge +
                ", profession=" + profession +
                ", phones=" + phones +
                "} " + super.toString();
    }
}
