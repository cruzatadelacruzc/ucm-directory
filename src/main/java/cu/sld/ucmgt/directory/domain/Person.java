package cu.sld.ucmgt.directory.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Person extends AbstractAuditingEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    protected UUID id;

    @Pattern(regexp = "(^[1-9]\\d{10}$)")
    protected String ci;

    @NotBlank
    protected String name;

    @Email
    protected String email;

    protected String avatarUrl;

    @NotBlank
    protected String address;

    protected String firstLastName;

    protected String secondLastName;

    @Enumerated(EnumType.STRING)
    protected Gender gender;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @Field(type = FieldType.Date, format = DateFormat.basic_date)
    protected LocalDate birthdate;

    /**
     * {@docRoot Black or White race = Black or White people}
     */
    @NotBlank
    protected String race;

    @ManyToOne
    @JsonIgnoreProperties(value = "peopleDistrict", allowSetters = true)
    protected Nomenclature district;

    @ManyToOne
    @JsonIgnoreProperties(value = "peopleSpecialty", allowSetters = true)
    protected Nomenclature specialty;

    @Override
    public String toString() {
        return "Person{" +
                "id=" + id +
                ", ci='" + ci + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", address='" + address + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", firstLastName='" + firstLastName + '\'' +
                ", secondLastName='" + secondLastName + '\'' +
                ", gender=" + gender +
                ", birthdate=" + birthdate +
                ", race='" + race + '\'' +
                '}';
    }
}
