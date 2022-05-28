package cu.sld.ucmgt.directory.service.dto;

import com.google.common.base.Objects;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.UUID;

/**
 * Class DTO representing a {@link cu.sld.ucmgt.directory.domain.Student} entity
 */
@Data
public class StudentDTO extends PersonDTO {
    @NotBlank(message = "error:NotBlank")
    private String classRoom;
    @Min(value = 1, message = "error:Min")
    private Integer universityYear;
    @NotBlank(message = "error:NotBlank")
    private String residence;
    private UUID kindId;
    private UUID studyCenterId;
    private String kindName;
    private String studyCenterName;

    private NomenclatureDTO studyCenter;
    private NomenclatureDTO kind;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudentDTO)) return false;
        if (!super.equals(o)) return false;
        StudentDTO that = (StudentDTO) o;
        return Objects.equal(classRoom, that.classRoom) &&
                Objects.equal(universityYear, that.universityYear) &&
                Objects.equal(residence, that.residence);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), classRoom, universityYear, residence);
    }

    @Override
    public String toString() {
        return "StudentDTO{" +
                "classRoom='" + classRoom + '\'' +
                ", universityYear=" + universityYear +
                ", residence='" + residence + '\'' +
                ", kindId=" + kindId +
                ", studyCenterId=" + studyCenterId +
                ", kindName='" + kindName + '\'' +
                ", studyCenterName='" + studyCenterName + '\'' +
                ", kind='" + kind + '\'' +
                ", studyCenter='" + studyCenter + '\'' +
                "} " + super.toString();
    }
}
