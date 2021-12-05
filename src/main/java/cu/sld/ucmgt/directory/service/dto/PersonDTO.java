package cu.sld.ucmgt.directory.service.dto;

import cu.sld.ucmgt.directory.domain.Gender;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Class DTO representing a {@link cu.sld.ucmgt.directory.domain.Person} entity
 */
@Data
public abstract class PersonDTO implements Serializable {
    protected UUID id;

    @Pattern(regexp = "(^[1-9]\\d{10}$)", /*message = "{validation.ci.malformed}"*/ message = "error:Pattern")
    protected String ci;
    @NotBlank(message = "error:NotBlank")
    protected String name;
    @NotBlank(message = "error:NotBlank")
    protected String address;
    protected String firstLastName;
    protected String secondLastName;
    @Email(message = "error:Email")
    protected String email;
    protected Gender gender;
    @NotBlank(message = "error:NotBlank")
    protected String race;
    protected UUID districtId;
    protected UUID specialtyId;
    protected LocalDate birthdate;

    protected String districtName;
    protected String specialtyName;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PersonDTO)) return false;

        return id != null && id.equals(((PersonDTO) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public String toString() {
        return "PersonDTO{" +
                "id=" + id +
                ", ci='" + ci + '\'' +
                ", name='" + name + '\'' +
                ", firstLastName='" + firstLastName + '\'' +
                ", secondLastName='" + secondLastName + '\'' +
                ", email=" + email +
                ", gender=" + gender +
                ", race='" + race + '\'' +
                ", districtId=" + districtId +
                ", districtName=" + districtName +
                ", specialtyName=" + specialtyName +
                '}';
    }
}
