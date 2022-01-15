package cu.sld.ucmgt.directory.domain;

import lombok.Data;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Data
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class WorkPlace extends AbstractAuditingEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank
    private String name;

    private Boolean active;

    @Email
    private String email;

    private String avatarUrl;

    private String description;

    @OneToMany(mappedBy = "workPlace")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Employee> employees = new HashSet<>();

    @OneToMany(mappedBy = "workPlace", cascade = CascadeType.PERSIST, orphanRemoval = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Phone> phones = new HashSet<>();

    public void removeEmployee(Employee employee) {
        this.employees.remove(employee);
        employee.setBossWorkPlace(false);
        employee.setWorkPlace(null);
    }

    public void removePhone(Phone phone) {
        this.phones.remove(phone);
        phone.setWorkPlace(null);
    }

    public void addEmployee(Employee employee) {
        this.employees.add(employee);
        employee.setWorkPlace(this);
    }

    public void addPhone(Phone phone) {
        this.phones.add(phone);
        phone.setWorkPlace(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkPlace)) return false;
        return id != null && id.equals(((WorkPlace) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "WorkPlace{" +
                "uid=" + id +
                ", name='" + name + '\'' +
                ", email=" + email +
                ", active=" + active +
                ", avatarUrl=" + avatarUrl +
                ", description='" + description + '\'' +
                '}';
    }
}
