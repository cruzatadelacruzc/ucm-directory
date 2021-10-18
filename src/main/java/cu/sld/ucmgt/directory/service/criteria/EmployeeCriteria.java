package cu.sld.ucmgt.directory.service.criteria;


import cu.sld.ucmgt.directory.domain.Employee;
import cu.sld.ucmgt.directory.service.filter.*;
import cu.sld.ucmgt.directory.web.rest.EmployeeResource;
import lombok.Data;

import java.util.Objects;


/**
 * Criteria class for the {@link Employee} entity. This class is used
 * in {@link EmployeeResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /employees/or?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@Data
public class EmployeeCriteria extends PersonCriteria {

    private static final long serialVersionUID = 1L;

    private StringFilter chargeName;
    private IntegerFilter serviceYears;
    private StringFilter workPlaceName;
    private StringFilter categoryName;
    private IntegerFilter graduateYears;
    private StringFilter registerNumber;
    private BooleanFilter bossWorkPlace;
    private StringFilter professionName;
    private LocalDateTimeFilter endDate;
    private LocalDateTimeFilter startDate;
    private StringFilter professionalNumber;
    private BooleanFilter isGraduatedBySector;
    private StringFilter scientificDegreeName;
    private StringFilter teachingCategoryName;

    public EmployeeCriteria() {}

    public EmployeeCriteria(EmployeeCriteria criteria) {
        super(criteria);
        this.endDate = criteria.endDate == null ? null : criteria.endDate.copy();
        this.startDate = criteria.startDate == null ? null : criteria.startDate.copy();
        this.chargeName = criteria.chargeName == null ? null : criteria.chargeName.copy();
        this.categoryName = criteria.categoryName == null ? null : criteria.categoryName.copy();
        this.serviceYears = criteria.serviceYears == null ? null : criteria.serviceYears.copy();
        this.bossWorkPlace = criteria.bossWorkPlace == null ? null : criteria.bossWorkPlace.copy();
        this.workPlaceName = criteria.workPlaceName == null ? null : criteria.workPlaceName.copy();
        this.graduateYears = criteria.graduateYears == null ? null : criteria.graduateYears.copy();
        this.registerNumber = criteria.registerNumber == null ? null : criteria.registerNumber.copy();
        this.professionName = criteria.professionName == null ? null : criteria.professionName.copy();
        this.professionalNumber = criteria.professionalNumber == null ? null : criteria.professionalNumber.copy();
        this.isGraduatedBySector = criteria.isGraduatedBySector == null ? null : criteria.isGraduatedBySector.copy();
        this.scientificDegreeName = criteria.scientificDegreeName == null ? null : criteria.scientificDegreeName.copy();
        this.teachingCategoryName = criteria.teachingCategoryName == null ? null : criteria.teachingCategoryName.copy();
    }

    public EmployeeCriteria copy() {
        return new EmployeeCriteria(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        EmployeeCriteria that = (EmployeeCriteria) o;
        return endDate.equals(that.endDate) &&
                Objects.equals(startDate, that.startDate) &&
                Objects.equals(chargeName, that.chargeName) &&
                Objects.equals(categoryName, that.categoryName) &&
                Objects.equals(serviceYears, that.serviceYears) &&
                Objects.equals(bossWorkPlace, that.bossWorkPlace) &&
                Objects.equals(workPlaceName, that.workPlaceName) &&
                Objects.equals(graduateYears, that.graduateYears) &&
                Objects.equals(registerNumber, that.registerNumber) &&
                Objects.equals(professionName, that.professionName) &&
                Objects.equals(professionalNumber, that.professionalNumber) &&
                Objects.equals(isGraduatedBySector, that.isGraduatedBySector) &&
                Objects.equals(scientificDegreeName, that.scientificDegreeName) &&
                Objects.equals(teachingCategoryName, that.teachingCategoryName);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (endDate != null ? endDate.hashCode() : 0);
        result = 31 * result + (chargeName != null ? chargeName.hashCode() : 0);
        result = 31 * result + (startDate != null ? startDate.hashCode() : 0);
        result = 31 * result + (serviceYears != null ? serviceYears.hashCode() : 0);
        result = 31 * result + (workPlaceName != null ? workPlaceName.hashCode() : 0);
        result = 31 * result + (categoryName != null ? categoryName.hashCode() : 0);
        result = 31 * result + (graduateYears != null ? graduateYears.hashCode() : 0);
        result = 31 * result + (registerNumber != null ? registerNumber.hashCode() : 0);
        result = 31 * result + (bossWorkPlace != null ? bossWorkPlace.hashCode() : 0);
        result = 31 * result + (professionName != null ? professionName.hashCode() : 0);
        result = 31 * result + (professionalNumber != null ? professionalNumber.hashCode() : 0);
        result = 31 * result + (isGraduatedBySector != null ? isGraduatedBySector.hashCode() : 0);
        result = 31 * result + (scientificDegreeName != null ? scientificDegreeName.hashCode() : 0);
        result = 31 * result + (teachingCategoryName != null ? teachingCategoryName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "EmployeeCriteria{" +
                "endDate=" + endDate +
                ", chargeName=" + chargeName +
                ", startDate=" + startDate +
                ", serviceYears=" + serviceYears +
                ", workPlaceName=" + workPlaceName +
                ", categoryName=" + categoryName +
                ", graduateYears=" + graduateYears +
                ", registerNumber=" + registerNumber +
                ", bossWorkPlace=" + bossWorkPlace +
                ", professionName=" + professionName +
                ", professionalNumber=" + professionalNumber +
                ", isGraduatedBySector=" + isGraduatedBySector +
                ", scientificDegreeName=" + scientificDegreeName +
                ", teachingCategoryName=" + teachingCategoryName +
                "} " + super.toString();
    }
}
