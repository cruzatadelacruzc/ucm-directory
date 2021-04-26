package cu.sld.ucmgt.directory.service.dto;

import cu.sld.ucmgt.directory.domain.Phone;
import cu.sld.ucmgt.directory.service.UniqueValue;
import lombok.Data;

import javax.validation.constraints.Min;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cu.sld.ucmgt.directory.domain.Phone} entity.
 */
@Data
@UniqueValue(entityClass = Phone.class, columnNames = "number", includeFields = "id")
public class PhoneDTO implements Serializable {

    private UUID id;
    @Min(value = 1)
    private Integer number;
    private Boolean active = false;
    private String description;
    private UUID employeeId;
    private UUID workPlaceId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhoneDTO)) return false;

        return id != null && id.equals(((PhoneDTO) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PhoneDTO{" +
                "uid=" + id +
                ", number=" + number +
                ", active=" + active +
                ", description='" + description + '\'' +
                ", employeeId=" + employeeId +
                ", workPlaceId=" + workPlaceId +
                '}';
    }
}
