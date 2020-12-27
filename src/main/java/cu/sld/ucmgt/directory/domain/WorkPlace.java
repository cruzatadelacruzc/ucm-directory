package cu.sld.ucmgt.directory.domain;

import lombok.Data;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.elasticsearch.annotations.Document;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Entity
@Document(indexName = "workplaces")
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

    private String description;

    @OneToMany(mappedBy = "workPlace")
    private Set<Employee> employees = new HashSet<>();

    @OneToMany(mappedBy = "workPlace")
    private Set<Phone> phones = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkPlace)) return false;
        return id != null && id.equals(((WorkPlace) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public String toString() {
        return "WorkPlace{" +
                "uid=" + id +
                ", name='" + name + '\'' +
                ", email=" + email +
                ", active=" + active +
                ", description='" + description + '\'' +
                '}';
    }
}
