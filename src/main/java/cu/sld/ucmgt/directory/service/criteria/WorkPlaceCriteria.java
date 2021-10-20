package cu.sld.ucmgt.directory.service.criteria;

import cu.sld.ucmgt.directory.domain.WorkPlace;
import cu.sld.ucmgt.directory.service.filter.BooleanFilter;
import cu.sld.ucmgt.directory.service.filter.Filter;
import cu.sld.ucmgt.directory.service.filter.StringFilter;
import cu.sld.ucmgt.directory.service.filter.UUIDFilter;
import cu.sld.ucmgt.directory.web.rest.WorkPlaceResource;
import lombok.Data;

import java.io.Serializable;

/**
 * Criteria class for the {@link WorkPlace} entity. This class is used
 * in {@link WorkPlaceResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /workplaces/or?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@Data
public class WorkPlaceCriteria implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;
    private StringFilter name;
    private StringFilter email;
    private BooleanFilter active;
    private StringFilter description;

    public WorkPlaceCriteria() {
    }

    public WorkPlaceCriteria(WorkPlaceCriteria criteria) {
        this.id = criteria.id == null ? null : criteria.id.copy();
        this.name = criteria.name == null ? null : criteria.name.copy();
        this.email = criteria.email == null ? null : criteria.email.copy();
        this.active = criteria.active == null ? null : criteria.active.copy();
        this.description = criteria.description == null ? null : criteria.description.copy();
    }

    public WorkPlaceCriteria copy() {
        return new WorkPlaceCriteria(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkPlaceCriteria that = (WorkPlaceCriteria) o;
        return id.equals(that.id) &&
                name.equals(that.name) &&
                email.equals(that.email) &&
                active.equals(that.active) &&
                description.equals(that.description);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (active != null ? active.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "WorkPlaceCriteria{" +
                "id=" + id +
                ", name=" + name +
                ", email=" + email +
                ", active=" + active +
                ", description=" + description +
                '}';
    }
}
