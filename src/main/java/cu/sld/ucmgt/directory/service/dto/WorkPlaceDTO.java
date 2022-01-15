package cu.sld.ucmgt.directory.service.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

/**
 * A DTO for the {@link cu.sld.ucmgt.directory.domain.WorkPlace} entity.
 */
@Data
public class WorkPlaceDTO implements Serializable {

    private UUID id;
    @NotBlank(message = "error:NotBlank")
    private String name;

    @Email(message = "error:Email")
    private String email;

    private Boolean active = false;

    private String description;

    private String avatarUrl;

    private Set<UUID> employeeIds;

    private Set<UUID> phoneIds;

    private Set<EmployeeDTO> employees;

    private Set<PhoneDTO> phones;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkPlaceDTO)) return false;

        return id != null && id.equals(((WorkPlaceDTO) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public String toString() {
        return "WorkPlaceDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", active=" + active +
                ", description='" + description + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", employeeIds=" + employeeIds +
                ", phoneIds=" + phoneIds +
                ", employees=" + employees +
                ", phones=" + phones +
                '}';
    }
}
