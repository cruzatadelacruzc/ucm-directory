package cu.sld.ucmgt.directory.service.dto;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudentDTO)) return false;

        return id != null && id.equals(((StudentDTO) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public String toString() {
        return "StudentDTO{" +
                "classRoom='" + classRoom + '\'' +
                ", universityYear=" + universityYear +
                ", residence='" + residence + '\'' +
                ", id=" + id +
                ", ci='" + ci + '\'' +
                ", name='" + name + '\'' +
                ", firstLastName='" + firstLastName + '\'' +
                ", secondLastName='" + secondLastName + '\'' +
                ", email='" + email + '\'' +
                ", gender=" + gender +
                ", age=" + age +
                ", birthdate=" + birthdate +
                ", race='" + race + '\'' +
                ", districtId=" + districtId +
                '}';
    }
}
