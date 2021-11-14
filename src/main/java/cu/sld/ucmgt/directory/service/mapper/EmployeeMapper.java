package cu.sld.ucmgt.directory.service.mapper;

import cu.sld.ucmgt.directory.domain.Employee;
import cu.sld.ucmgt.directory.domain.Phone;
import cu.sld.ucmgt.directory.service.dto.EmployeeDTO;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;
import java.util.UUID;

/**
 * Mapper for the entity {@link Employee} and its DTO {@link EmployeeDTO}.
 */
@Mapper(componentModel = "spring", uses = {NomenclatureMapper.class, WorkPlaceMapper.class})
public interface EmployeeMapper extends EntityMapper<EmployeeDTO, Employee> {

    @Mapping(target = "phones", ignore = true)
    @Mapping(source = "chargeId", target = "charge")
    @Mapping(source = "districtId", target = "district")
    @Mapping(source = "categoryId", target = "category")
    @Mapping(source = "workPlaceId", target = "workPlace")
    @Mapping(source = "specialtyId", target = "specialty")
    @Mapping(source = "professionId", target = "profession")
    @Mapping(source = "scientificDegreeId", target = "scientificDegree")
    @Mapping(source = "teachingCategoryId", target = "teachingCategory")
    Employee toEntity(EmployeeDTO dto);

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

    @IterableMapping(elementTargetType = String.class)
    Set<String> mapToPhoneToString(Set<Phone> phones);

    default String mapPhoneToInteger(Phone phone) {
        if (phone == null) {
            return null;
        }
        return phone.getNumber();
    }

    default Employee fromId(UUID id) {
        if (id == null) {
            return null;
        }
        Employee employee = new Employee();
        employee.setId(id);
        return employee;
    }
}
