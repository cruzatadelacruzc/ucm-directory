package cu.sld.ucmgt.directory.domain;

import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;
import javax.persistence.*;
import java.io.Serializable;
import javax.validation.constraints.NotBlank;

@Data
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Nomenclature implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank
    private String name;

    private Boolean active;

    private String description;

    @Enumerated(value = EnumType.STRING)
    private NomenclatureType discriminator;

    @OneToMany(mappedBy = "charge")
    private Set<Employee> employeesCharge = new HashSet<>();

    @OneToMany(mappedBy = "category")
    private Set<Employee> employeesCategory = new HashSet<>();

    @OneToMany(mappedBy = "specialty")
    private Set<Person> peopleSpecialty = new HashSet<>();

    @OneToMany(mappedBy = "career")
    private Set<Student> studentCareer = new HashSet<>();

    @OneToMany(mappedBy = "kind")
    private Set<Student> studentsKind = new HashSet<>();

    @OneToMany(mappedBy = "scientificDegree")
    private Set<Employee> employeesScientificDegree = new HashSet<>();

    @OneToMany(mappedBy = "teachingCategory")
    private Set<Employee> employeesTeachingCategory = new HashSet<>();

    @OneToMany(mappedBy = "profession")
    private Set<Employee> employeesProfession = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Nomenclature)) return false;
        return id != null && id.equals(((Nomenclature) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public String toString() {
        return "Nomenclature{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", active=" + active +
                ", description='" + description + '\'' +
                ", discriminator=" + discriminator +
                '}';
    }
}
