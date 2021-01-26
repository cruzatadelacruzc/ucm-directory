package cu.sld.ucmgt.directory.domain;

import lombok.Data;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
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

    @OneToOne
    private Nomenclature parent;

    private Boolean active;

    private String description;

    @Enumerated(value = EnumType.STRING)
    private NomenclatureType discriminator;


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
