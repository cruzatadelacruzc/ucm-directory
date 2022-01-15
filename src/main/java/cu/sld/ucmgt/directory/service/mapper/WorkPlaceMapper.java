package cu.sld.ucmgt.directory.service.mapper;

import cu.sld.ucmgt.directory.domain.Employee;
import cu.sld.ucmgt.directory.domain.Phone;
import cu.sld.ucmgt.directory.domain.WorkPlace;
import cu.sld.ucmgt.directory.service.dto.WorkPlaceDTO;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;
import java.util.UUID;

/**
 * Mapper for the entity {@link WorkPlace} and its DTO {@link WorkPlaceDTO}.
 */
@Mapper(componentModel = "spring", uses = {EmployeeMapper.class, PhoneMapper.class})
public interface WorkPlaceMapper extends EntityMapper<WorkPlaceDTO, WorkPlace> {

    @Mapping(target = "phones", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "employees", ignore = true)
    WorkPlace toEntity(WorkPlaceDTO dto);

    @Mapping(target = "phones", ignore = true)
    @Mapping(target = "employees", ignore = true)
    @Mapping(source = "phones", target = "phoneIds")
    @Mapping(source = "employees", target = "employeeIds")
    WorkPlaceDTO toDto(WorkPlace entity);

    @IterableMapping(elementTargetType = UUID.class)
    Set<UUID> mapToEmployeeToUUID(Set<Employee> employees);

    @IterableMapping(elementTargetType = UUID.class)
    Set<UUID> mapToPhoneToUUID(Set<Phone> phones);

    default UUID mapEmployeeToUUID(Employee employee) {
        if (employee == null) {
            return null;
        }
        return employee.getId();
    }

    default UUID mapPhoneToUUID(Phone phone) {
        if (phone == null) {
            return null;
        }
        return phone.getId();
    }

    default WorkPlace fromId(UUID uid) {
        if (uid == null) {
            return null;
        }
        WorkPlace workPlace = new WorkPlace();
        workPlace.setId(uid);
        return workPlace;
    }
}
