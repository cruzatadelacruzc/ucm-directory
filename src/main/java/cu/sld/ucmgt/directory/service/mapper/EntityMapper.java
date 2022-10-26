package cu.sld.ucmgt.directory.service.mapper;

import cu.sld.ucmgt.directory.domain.Gender;
import cu.sld.ucmgt.directory.domain.Person;
import cu.sld.ucmgt.directory.service.dto.PersonDTO;
import org.mapstruct.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Set;

/**
 * Contract for a generic dto to entity mapper.
 *
 * @param <D> - DTO type parameter.
 * @param <E> - domain type parameter.
 */
public interface EntityMapper<D,E> {

    D toDto(E entity);

    E toEntity(D dto);

    List<E> toEntities(List<D> dtos);

    Set<E> toEntities(Set<D> dtos);

    List<D> toDtos(List<E> entities);

    Set<D> toDtos(Set<E> entities);

    @Named("partialUpdate")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void partialUpdate(D dto, @MappingTarget E entity);

    @AfterMapping
    default void setBirthDateFromCI(D dto, @MappingTarget E entity) {
        if ((dto instanceof PersonDTO) && (entity instanceof Person)) {
            if (((PersonDTO) dto).getCi() != null && ((PersonDTO) dto).getCi().length() == 11 && ((PersonDTO) dto).getBirthdate() == null) {
                try {
                    String date1 = String.format("%s%s%s", ((PersonDTO) dto).getCi().substring(4, 6), ((PersonDTO) dto).getCi().substring(2, 4), ((PersonDTO) dto).getCi().substring(0, 2));
                    int era = Integer.parseInt(((PersonDTO) dto).getCi().substring(6, 7));
                    DateTimeFormatter format = new DateTimeFormatterBuilder()
                            .appendPattern("ddMM")
                            .appendValueReduced(ChronoField.YEAR, 2, 2, era <= 5 ? 1900 : 2000)
                            .toFormatter();
                    ((Person) entity).setBirthdate(LocalDate.parse(date1, format));
                } catch (DateTimeParseException e) {
                    ((Person) entity).setBirthdate(LocalDate.now());
                }
            }
        }

    }

    @AfterMapping
    default void setGenderFromCI(D dto, @MappingTarget E entity) {
        if ((dto instanceof PersonDTO) && (entity instanceof Person)) {
            if (((PersonDTO) dto).getCi() != null && ((PersonDTO) dto).getCi().length() == 11 && ((PersonDTO) dto).getGender() == null) {
                String genderDigit = ((PersonDTO) dto).getCi().substring(9, 10);
                Gender gender = Integer.parseInt(genderDigit) % 2 == 0 ? Gender.Masculino : Gender.Femenino;
                ((Person) entity).setGender(gender);
            }
        }
    }

}