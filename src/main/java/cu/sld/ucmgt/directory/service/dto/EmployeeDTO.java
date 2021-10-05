package cu.sld.ucmgt.directory.service.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Class DTO representing a {@link cu.sld.ucmgt.directory.domain.Employee} entity
 */
@Data
public class EmployeeDTO extends PersonDTO {

    @NotNull(message = "error:NotNull")
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean bossWorkPlace;
    @Min(value = 0, message = "error:Min")
    private Integer graduateYears;
    private Boolean isGraduatedBySector;
    @Min(value = 0, message = "error:Min")
    private Integer serviceYears;
    @NotBlank(message = "error:NotBlank")
    private String registerNumber;
    private String professionalNumber;
    private UUID workPlaceId;
    private UUID categoryId;
    private UUID scientificDegreeId;
    private UUID teachingCategoryId;
    private UUID chargeId;
    private UUID professionId;

    private String workPlaceName;
    private String categoryName;
    private String scientificDegreeName;
    private String teachingCategoryName;
    private String chargeName;
    private String professionName;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmployeeDTO)) return false;

        return id != null && id.equals(((EmployeeDTO) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public String toString() {
        return "EmployeeDTO{" +
                "address='" + address + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", bossWorkPlace=" + bossWorkPlace +
                ", graduateYears=" + graduateYears +
                ", isGraduatedBySector=" + isGraduatedBySector +
                ", serviceYears=" + serviceYears +
                ", workPlaceId=" + workPlaceId +
                ", specialtyId=" + specialtyId +
                ", categoryId=" + categoryId +
                ", scientificDegreeId=" + scientificDegreeId +
                ", teachingCategoryId=" + teachingCategoryId +
                ", chargeId=" + chargeId +
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
                ", registerNumber='" + registerNumber + '\'' +
                ", professionalNumber='" + professionalNumber + '\'' +
                ", professionId=" + professionId +
                ", workPlaceName=" + workPlaceName +
                ", categoryName=" + categoryName +
                ", scientificDegreeName=" + scientificDegreeName +
                ", teachingCategoryName=" + teachingCategoryName +
                ", chargeName=" + chargeName +
                ", professionName=" + professionName +
                '}';
    }
}
