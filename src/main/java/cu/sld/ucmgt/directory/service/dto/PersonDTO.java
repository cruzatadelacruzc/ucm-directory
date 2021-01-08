package cu.sld.ucmgt.directory.service.dto;

import cu.sld.ucmgt.directory.domain.Gender;
import lombok.Data;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.Email;
import javax.validation.constraints.Min;
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

    @Pattern(regexp = "(^[1-9]\\d{10}$)")
    protected String CI;
    @NotBlank
    protected String name;
    @NotBlank
    protected String address;
    protected String firstLastName;
    protected String secondLastName;
    @Email
    protected String email;
    protected Gender gender;
    @Min(value = 14)
    protected Integer age;
    @NotBlank
    protected String race;
    protected UUID districtId;
    protected LocalDate birthdate;

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
                ", CI='" + CI + '\'' +
                ", name='" + name + '\'' +
                ", firstLastName='" + firstLastName + '\'' +
                ", secondLastName='" + secondLastName + '\'' +
                ", email=" + email +
                ", gender=" + gender +
                ", age=" + age +
                ", race='" + race + '\'' +
                ", districtId=" + districtId +
                '}';
    }
}
