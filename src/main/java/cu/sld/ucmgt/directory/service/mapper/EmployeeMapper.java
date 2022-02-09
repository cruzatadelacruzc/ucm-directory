package cu.sld.ucmgt.directory.service.mapper;

import cu.sld.ucmgt.directory.domain.Employee;
import cu.sld.ucmgt.directory.domain.Gender;
import cu.sld.ucmgt.directory.service.dto.EmployeeDTO;
import org.mapstruct.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.UUID;

/**
 * Mapper for the entity {@link Employee} and its DTO {@link EmployeeDTO}.
 */
@Mapper(componentModel = "spring", uses = {NomenclatureMapper.class, WorkPlaceMapper.class})
public interface EmployeeMapper extends EntityMapper<EmployeeDTO, Employee> {

    @Mapping(target = "phones", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(source = "chargeId", target = "charge")
    @Mapping(source = "districtId", target = "district")
    @Mapping(source = "categoryId", target = "category")
    @Mapping(source = "workPlaceId", target = "workPlace")
    @Mapping(source = "specialtyId", target = "specialty")
    @Mapping(source = "professionId", target = "profession")
    @Mapping(source = "scientificDegreeId", target = "scientificDegree")
    @Mapping(source = "teachingCategoryId", target = "teachingCategory")
    Employee toEntity(EmployeeDTO dto);

    @Mapping(target = "phones", ignore = true)
    @Mapping(source = "charge.id", target = "chargeId")
    @Mapping(source = "district.id", target = "districtId")
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "workPlace.id", target = "workPlaceId")
    @Mapping(source = "specialty.id", target = "specialtyId")
    @Mapping(source = "profession.id", target = "professionId")
    @Mapping(source = "scientificDegree.id", target = "scientificDegreeId")
    @Mapping(source = "teachingCategory.id", target = "teachingCategoryId")
    @Mapping(source = "charge.name", target = "chargeName")
    @Mapping(source = "district.name", target = "districtName")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "workPlace.name", target = "workPlaceName")
    @Mapping(source = "specialty.name", target = "specialtyName")
    @Mapping(source = "profession.name", target = "professionName")
    @Mapping(source = "scientificDegree.name", target = "scientificDegreeName")
    @Mapping(source = "teachingCategory.name", target = "teachingCategoryName")
    EmployeeDTO toDto(Employee entity);


    @Mapping(target = "phones", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(source = "chargeId", target = "charge")
    @Mapping(source = "districtId", target = "district")
    @Mapping(source = "categoryId", target = "category")
    @Mapping(source = "workPlaceId", target = "workPlace")
    @Mapping(source = "specialtyId", target = "specialty")
    @Mapping(source = "professionId", target = "profession")
    @Mapping(source = "scientificDegreeId", target = "scientificDegree")
    @Mapping(source = "teachingCategoryId", target = "teachingCategory")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void partialUpdate(EmployeeDTO dto, @MappingTarget Employee employee);


    default Employee fromId(UUID id) {
        if (id == null) {
            return null;
        }
        Employee employee = new Employee();
        employee.setId(id);
        return employee;
    }
    @AfterMapping
    default void setBirthDateFromCI(EmployeeDTO dto  , @MappingTarget Employee entity) {
        if (dto.getCi() != null && dto.getCi().length() == 11 && dto.getBirthdate() == null) {
            try {
//                String date = String.format("%s%s%s", dto.getCi().substring(0, 2), dto.getCi().substring(2, 4), dto.getCi().substring(4, 6));
//                SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd");
//                LocalDate birthdate = dateFormat.parse(date).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            String date1 = String.format("%s%s%s", dto.getCi().substring(4, 6), dto.getCi().substring(2, 4), dto.getCi().substring(0, 2));
            int era = Integer.parseInt(dto.getCi().substring(6,7));
            DateTimeFormatter format = new DateTimeFormatterBuilder()
                    .appendPattern("ddMM")
                    .appendValueReduced(ChronoField.YEAR, 2, 2, era <= 5 ? 1900: 2000)
                    .toFormatter();
                entity.setBirthdate(LocalDate.parse(date1, format));
            } catch (DateTimeParseException e) {
                entity.setBirthdate(LocalDate.now());
            }
        }

    }

    @AfterMapping
    default void setGenderFromCI(EmployeeDTO dto  , @MappingTarget Employee entity) {
        if (dto.getCi() != null && dto.getCi().length() == 11 && dto.getGender() == null) {
            String genderDigit = dto.getCi().substring(9,10);
           Gender gender = Integer.parseInt(genderDigit) %2 == 0 ? Gender.Masculino: Gender.Femenino;
           entity.setGender(gender);
        }
    }
}
