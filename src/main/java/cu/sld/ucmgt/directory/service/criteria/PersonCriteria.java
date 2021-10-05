package cu.sld.ucmgt.directory.service.criteria;

import cu.sld.ucmgt.directory.domain.Gender;
import cu.sld.ucmgt.directory.domain.Person;
import cu.sld.ucmgt.directory.service.filter.*;
import cu.sld.ucmgt.directory.web.rest.EmployeeResource;
import cu.sld.ucmgt.directory.web.rest.StudentResource;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

/**
 * Criteria class for the {@link Person} entity. This class is used
 * in {@link EmployeeResource} and  {@link StudentResource}to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /employees?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@Data
public class PersonCriteria implements Serializable {

    protected static final long serialVersionUID = 1L;

    protected UUIDFilter id;
    protected IntegerFilter age;
    protected StringFilter name;
    protected StringFilter race;
    protected StringFilter email;
    protected GenderFilter gender;
    protected StringFilter address;
    protected StringFilter districtName;
    protected LocalDateFilter birthdate;
    protected StringFilter specialtyName;
    protected StringFilter firstLastName;
    protected StringFilter secondLastName;

    public PersonCriteria() {
    }

    public PersonCriteria(PersonCriteria criteria) {
        this.id = criteria.id == null ?  null : criteria.id.copy();
        this.age = criteria.age == null ? null: criteria.age.copy();
        this.race = criteria.race == null ? null: criteria.race.copy();
        this.name  = criteria.name == null ? null: criteria.name.copy();
        this.email = criteria.email == null ? null: criteria.email.copy();
        this.gender = criteria.gender == null ? null: criteria.gender.copy();
        this.address = criteria.address == null ? null: criteria.address.copy();
        this.birthdate = criteria.birthdate == null? null: criteria.birthdate.copy();
        this.districtName = criteria.districtName == null ? null : criteria.districtName.copy();
        this.specialtyName = criteria.specialtyName == null ? null : criteria.specialtyName.copy();
        this.firstLastName = criteria.firstLastName == null ? null : criteria.firstLastName.copy();
        this.secondLastName = criteria.secondLastName == null ? null: criteria.secondLastName.copy();
    }

    public PersonCriteria copy() {
        return new PersonCriteria(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersonCriteria that = (PersonCriteria) o;
        return id.equals(that.id) &&
                Objects.equals(age, that.age) &&
                Objects.equals(name, that.name) &&
                Objects.equals(race, that.race) &&
                Objects.equals(email, that.email) &&
                Objects.equals(gender, that.gender) &&
                Objects.equals(address, that.address) &&
                Objects.equals(birthdate, that.birthdate) &&
                Objects.equals(districtName, that.districtName) &&
                Objects.equals(specialtyName, that.specialtyName) &&
                Objects.equals(firstLastName, that.firstLastName) &&
                Objects.equals(secondLastName, that.secondLastName);
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (age != null ? age.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (race != null ? race.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (gender != null ? gender.hashCode() : 0);
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + (districtName != null ? districtName.hashCode() : 0);
        result = 31 * result + (birthdate != null ? birthdate.hashCode() : 0);
        result = 31 * result + (specialtyName != null ? specialtyName.hashCode() : 0);
        result = 31 * result + (firstLastName != null ? firstLastName.hashCode() : 0);
        result = 31 * result + (secondLastName != null ? secondLastName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PersonCriteria{" +
                "id=" + id +
                ", age=" + age +
                ", name=" + name +
                ", race=" + race +
                ", email=" + email +
                ", gender=" + gender +
                ", address=" + address +
                ", districtName=" + districtName +
                ", birthdate=" + birthdate +
                ", specialtyName=" + specialtyName +
                ", firstLastName=" + firstLastName +
                ", secondLastName=" + secondLastName +
                '}';
    }

    /**
     * Class for filtering Gender
     */
    public static class GenderFilter extends Filter<Gender> {
        public GenderFilter() {
        }

        public GenderFilter(GenderFilter filter) {
            super(filter);
        }

        public GenderFilter copy() {
            return new GenderFilter(this);
        }
    }
}
