package cu.sld.ucmgt.directory.service.criteria;

import cu.sld.ucmgt.directory.domain.Nomenclature;
import cu.sld.ucmgt.directory.service.filter.Filter;
import cu.sld.ucmgt.directory.service.filter.StringFilter;
import cu.sld.ucmgt.directory.service.filter.UUIDFilter;
import cu.sld.ucmgt.directory.web.rest.NomenclatureResource;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

/**
 * Criteria class for the {@link Nomenclature} entity. This class is used
 * in {@link NomenclatureResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /nomenclatures/or?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@Data
public class NomenclatureCriteria implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUIDFilter id;
    private StringFilter name;
    private StringFilter description;
    private StringFilter discriminator;

    public NomenclatureCriteria() {}

    public NomenclatureCriteria(NomenclatureCriteria criteria) {
        this.id = criteria.id == null ? null: criteria.id.copy();
        this.name = criteria.name == null ? null : criteria.name.copy();
        this.description = criteria.description == null ? null : criteria.description.copy();
        this.discriminator = criteria.discriminator == null ? null : criteria.discriminator.copy();
    }

    public NomenclatureCriteria copy() {
        return new NomenclatureCriteria(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NomenclatureCriteria that = (NomenclatureCriteria) o;
        return id.equals(that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(discriminator, that.discriminator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, discriminator);
    }

    @Override
    public String toString() {
        return "NomenclatureCriteria{" +
                "id=" + id +
                ", name=" + name +
                ", description=" + description +
                ", discriminator=" + discriminator +
                '}';
    }
}
