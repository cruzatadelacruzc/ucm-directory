package cu.sld.ucmgt.directory.service.criteria;

import com.google.common.base.Objects;
import cu.sld.ucmgt.directory.domain.Student;
import cu.sld.ucmgt.directory.service.filter.Filter;
import cu.sld.ucmgt.directory.service.filter.IntegerFilter;
import cu.sld.ucmgt.directory.service.filter.StringFilter;
import cu.sld.ucmgt.directory.web.rest.StudentResource;
import lombok.Data;

/**
 * Criteria class for the {@link Student} entity. This class is used
 * in {@link StudentResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /students/or?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@Data
public class StudentCriteria extends PersonCriteria {

    private static final long serialVersionUID = 1L;

    private StringFilter kindName;
    private StringFilter classRoom;
    private StringFilter residence;
    private StringFilter studyCenterName;
    private IntegerFilter universityYear;

    public StudentCriteria() {}

    public StudentCriteria(StudentCriteria criteria) {
        super(criteria);
        this.kindName = criteria.kindName == null ? null : criteria.kindName.copy();
        this.residence = criteria.residence == null? null : criteria.classRoom.copy();
        this.classRoom = criteria.classRoom == null ? null : criteria.classRoom.copy();
        this.universityYear = criteria.universityYear == null ? null : criteria.universityYear.copy();
        this.studyCenterName = criteria.studyCenterName == null ? null : criteria.studyCenterName.copy();
    }

    public StudentCriteria copy() {return  new StudentCriteria(this);}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudentCriteria)) return false;
        if (!super.equals(o)) return false;
        StudentCriteria that = (StudentCriteria) o;
        return Objects.equal(kindName, that.kindName) &&
                Objects.equal(classRoom, that.classRoom) &&
                Objects.equal(residence, that.residence) &&
                Objects.equal(studyCenterName, that.studyCenterName) &&
                Objects.equal(universityYear, that.universityYear);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), kindName, classRoom, residence, studyCenterName, universityYear);
    }

}
