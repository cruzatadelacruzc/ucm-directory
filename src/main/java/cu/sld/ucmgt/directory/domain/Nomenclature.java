package cu.sld.ucmgt.directory.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

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

    private String description;

    @Enumerated(value = EnumType.STRING)
    private NomenclatureType discriminator;

    @ManyToOne
    @JsonIgnoreProperties(value = "childrenDistrict", allowSetters = true)
    private Nomenclature parentDistrict;

    @OneToMany(mappedBy = "parentDistrict", orphanRemoval = true, cascade = CascadeType.PERSIST)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Nomenclature> childrenDistrict = new HashSet<>();

    @OneToMany(mappedBy = "district")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Person> peopleDistrict = new HashSet<>();

    @OneToMany(mappedBy = "specialty")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Person> peopleSpecialty = new HashSet<>();

    @OneToMany(mappedBy = "category")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Employee> employeesCategory = new HashSet<>();

    @OneToMany(mappedBy = "scientificDegree")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Employee> employeesScientificDegree = new HashSet<>();

    @OneToMany(mappedBy = "teachingCategory")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Employee> employeesTeachingCategory = new HashSet<>();

    @OneToMany(mappedBy = "charge")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Employee> employeesCharge = new HashSet<>();

    @OneToMany(mappedBy = "profession")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Employee> employeesProfession = new HashSet<>();

    @OneToMany(mappedBy = "kind")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Student> studentsKind = new HashSet<>();

    @OneToMany(mappedBy = "studyCenter")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Student> studentsStudyCenter = new HashSet<>();


    public Nomenclature removePeopleDistrict(Person person){
        this.peopleDistrict.remove(person);
        person.setDistrict(null);
        return this;
    }

    public Nomenclature removePeopleSpecialty(Person person){
        this.peopleSpecialty.remove(person);
        person.setSpecialty(null);
        return this;
    }

    public Nomenclature removeEmployeesCategory(Employee employee){
        this.employeesCategory.remove(employee);
        employee.setCategory(null);
        return this;
    }

    public Nomenclature removeEmployeesScientificDegree(Employee employee){
        this.employeesScientificDegree.remove(employee);
        employee.setScientificDegree(null);
        return this;
    }

    public Nomenclature removeEmployeesTeachingCategory(Employee employee){
        this.employeesTeachingCategory.remove(employee);
        employee.setTeachingCategory(null);
        return this;
    }

    public Nomenclature removeEmployeesCharge(Employee employee){
        this.employeesCharge.remove(employee);
        employee.setCharge(null);
        return this;
    }

    public Nomenclature removeEmployeesProfession(Employee employee){
        this.employeesProfession.remove(employee);
        employee.setProfession(null);
        return this;
    }

    public Nomenclature removeStudentsKind(Student student){
        this.studentsKind.remove(student);
        student.setKind(null);
        return this;
    }

    public Nomenclature removeStudentsStudyCenter(Student student){
        this.studentsStudyCenter.remove(student);
        student.setStudyCenter(null);
        return this;
    }

    public boolean isChildDistrict() {
        return parentDistrict != null && discriminator.equals(NomenclatureType.DISTRITO);
    }

    public void addNomenclatureDistrict(Nomenclature nomenclatureDistrict) {
        this.childrenDistrict.add(nomenclatureDistrict);
        nomenclatureDistrict.setParentDistrict(this);
    }

    public void addPeopleDistrict(Person person) {
        this.peopleDistrict.add(person);
        person.setDistrict(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Nomenclature)) return false;
        return id != null && id.equals(((Nomenclature) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Nomenclature{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", discriminator=" + discriminator +
                '}';
    }
}
