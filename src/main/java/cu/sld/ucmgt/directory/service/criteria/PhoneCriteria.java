package cu.sld.ucmgt.directory.service.criteria;

import com.google.common.base.Objects;
import cu.sld.ucmgt.directory.domain.Phone;
import cu.sld.ucmgt.directory.service.filter.BooleanFilter;
import cu.sld.ucmgt.directory.service.filter.Filter;
import cu.sld.ucmgt.directory.service.filter.StringFilter;
import cu.sld.ucmgt.directory.service.filter.UUIDFilter;
import cu.sld.ucmgt.directory.web.rest.PhoneResource;
import lombok.Data;

import java.io.Serializable;

/**
 * Criteria class for the {@link Phone} entity. This class is used
 * {@link PhoneResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /nomenclatures/or?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@Data
public class PhoneCriteria implements Serializable {

    protected static final long serialVersionUID = 1L;

    private UUIDFilter id;
    private StringFilter number;
    private BooleanFilter active;
    private StringFilter employeeName;
    private StringFilter workPlaceName;
    private StringFilter description;

    public PhoneCriteria() {
    }

    public PhoneCriteria(PhoneCriteria criteria) {
        this.id = criteria.id == null ? null : criteria.id.copy();
        this.number = criteria.number == null ? null : criteria.number.copy();
        this.active = criteria.active == null ? null : criteria.active.copy();
        this.employeeName = criteria.employeeName == null ? null : criteria.employeeName.copy();
        this.workPlaceName = criteria.workPlaceName == null ? null : criteria.workPlaceName.copy();
        this.description = criteria.description == null ? null : criteria.description.copy();
    }

    public PhoneCriteria copy() {
        return new PhoneCriteria(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhoneCriteria)) return false;
        PhoneCriteria that = (PhoneCriteria) o;
        return Objects.equal(id, that.id) &&
                Objects.equal(number, that.number) &&
                Objects.equal(active, that.active);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, number, active);
    }

    @Override
    public String toString() {
        return "PhoneCriteria{" +
                "id=" + id +
                ", number=" + number +
                ", active=" + active +
                ", employeeName=" + employeeName +
                ", workPlaceName=" + workPlaceName +
                ", description=" + description +
                '}';
    }
}
