package cu.sld.ucmgt.directory.domain;

import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.elasticsearch.annotations.Document;

import java.util.UUID;
import javax.persistence.*;
import java.io.Serializable;
import javax.validation.constraints.Min;

@Data
@Entity
@Document(indexName = "phones")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Phone extends AbstractAuditingEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Min(value = 1)
    private Integer number;

    private Boolean active;

    private String description;

    @ManyToOne
    @JsonIgnoreProperties(value = "phones", allowSetters = true)
    private Employee employee;

    @ManyToOne
    @JsonIgnoreProperties(value = "phones", allowSetters = true)
    private WorkPlace workPlace;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Phone)) return false;

        return id != null && id.equals(((Phone) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public String toString() {
        return "Phone{" +
                "uid=" + id +
                ", number=" + number +
                ", active=" + active +
                ", description='" + description + '\'' +
                '}';
    }
}
