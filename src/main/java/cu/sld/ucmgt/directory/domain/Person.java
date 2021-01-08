package cu.sld.ucmgt.directory.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Person extends AbstractAuditingEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    protected UUID id;

    @Pattern(regexp = "(^[1-9]\\d{10}$)")
    protected String CI;

    @NotBlank
    protected String name;

    @Email
    protected String email;

    @NotBlank
    private String address;

    protected String firstLastName;

    protected String secondLastName;

    @Enumerated(EnumType.STRING)
    protected Gender gender;

    @Min(value = 1)
    protected Integer age;

    protected LocalDate birthdate;

    /**
     * {@docRoot Black or White race = Black or White people}
     */
    @NotBlank
    protected String race;

    @ManyToOne
    @JsonIgnoreProperties(value = "personsDistrict")
    protected Nomenclature district;

    @ManyToOne
    @JsonIgnoreProperties(value = "peopleSpecialty")
    protected Nomenclature specialty;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Person)) return false;

        return id != null && id.equals(((Person) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public String toString() {
        return "Person{" +
                "id=" + id +
                ", CI='" + CI + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", address='" + address + '\'' +
                ", firstLastName='" + firstLastName + '\'' +
                ", secondLastName='" + secondLastName + '\'' +
                ", gender=" + gender +
                ", age=" + age +
                ", birthdate=" + birthdate +
                ", specialty=" + specialty +
                ", race='" + race + '\'' +
                '}';
    }
}
