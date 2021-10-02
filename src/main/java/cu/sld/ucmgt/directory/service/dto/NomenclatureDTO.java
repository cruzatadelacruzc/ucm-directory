package cu.sld.ucmgt.directory.service.dto;

import cu.sld.ucmgt.directory.domain.NomenclatureType;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.UUID;

/**
 * Class DTO representing a {@link cu.sld.ucmgt.directory.domain.Nomenclature} entity
 */
@Data
public class NomenclatureDTO {
    private UUID id;
    @NotBlank(message = "error:NotBlank")
    private String name;
    @Size(max = 255, message = "error:Size")
    private String description;
    private UUID parentDistrictId;
    private String parentDistrictName;
    private NomenclatureType discriminator;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NomenclatureDTO)) return false;

        return id != null && id.equals(((NomenclatureDTO) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public String toString() {
        return "NomenclatureDTO{" +
                "uid=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", discriminator=" + discriminator +
                ", parentDistrictId=" + parentDistrictId +
                ", parentDistrictName=" + parentDistrictName +
                '}';
    }
}
