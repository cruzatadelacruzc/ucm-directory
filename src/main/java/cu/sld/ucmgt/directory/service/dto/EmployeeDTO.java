package cu.sld.ucmgt.directory.service.dto;

import com.google.common.base.Objects;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Set;
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
    @Min(value = 0, message = "error:Min")
    private Integer salary;
    private String professionalNumber;
    private UUID workPlaceId;
    private UUID categoryId;
    private UUID scientificDegreeId;
    private UUID teachingCategoryId;
    private UUID chargeId;
    private UUID professionId;
    private Set<PhoneDTO> phones;

    private NomenclatureDTO category;
    private NomenclatureDTO scientificDegree;
    private NomenclatureDTO charge;
    private NomenclatureDTO profession;
    private NomenclatureDTO teachingCategory;
    private WorkPlaceDTO workPlace;

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
        if (!super.equals(o)) return false;
        EmployeeDTO that = (EmployeeDTO) o;
        return Objects.equal(registerNumber, that.registerNumber) &&
                Objects.equal(professionalNumber, that.professionalNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), registerNumber, professionalNumber);
    }

    @Override
    public String toString() {
        return "EmployeeDTO{" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                ", bossWorkPlace=" + bossWorkPlace +
                ", graduateYears=" + graduateYears +
                ", isGraduatedBySector=" + isGraduatedBySector +
                ", serviceYears=" + serviceYears +
                ", registerNumber='" + registerNumber +
                ", professionalNumber='" + professionalNumber +
                ", salary='" + salary +
                ", workPlace=" + workPlace +
                ", category=" + category +
                ", teachingCategory=" + teachingCategory +
                ", workPlaceId=" + workPlaceId +
                ", categoryId=" + categoryId +
                ", scientificDegreeId=" + scientificDegreeId +
                ", teachingCategoryId=" + teachingCategoryId +
                ", chargeId=" + chargeId +
                ", professionId=" + professionId +
                ", phones=" + phones +
                ", workPlaceName='" + workPlaceName +
                ", categoryName='" + categoryName +
                ", scientificDegreeName='" + scientificDegreeName +
                ", teachingCategoryName='" + teachingCategoryName +
                ", chargeName='" + chargeName +
                ", professionName='" + professionName +
                "} " + super.toString();
    }
}
