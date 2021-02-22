package cu.sld.ucmgt.directory.service.mapper;

import cu.sld.ucmgt.directory.domain.Employee;
import cu.sld.ucmgt.directory.domain.elasticsearch.EmployeeIndex;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for the index {@link EmployeeIndex} and its entity {@link Employee}.
 */
@Mapper(componentModel = "spring")
public interface EmployeeIndexMapper extends IndexMapper<EmployeeIndex, Employee>{

    @Mapping(source = "charge.name", target = "charge")
    @Mapping(source = "district.name", target = "district")
    @Mapping(source = "category.name", target = "category")
    @Mapping(source = "specialty.name", target = "specialty")
    @Mapping(source = "profession.name", target = "profession")
    @Mapping(source = "district.parentDistrict.name", target = "parentDistrict")
    @Mapping(target = "workPlace.employees", ignore = true)
    EmployeeIndex toIndex(Employee entity);


}
