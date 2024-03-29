package cu.sld.ucmgt.directory.service.mapper;

import cu.sld.ucmgt.directory.domain.Phone;
import cu.sld.ucmgt.directory.service.dto.PhoneDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

/**
 * Mapper for the entity {@link Phone} and its DTO {@link PhoneDTO}.
 */
@Mapper(componentModel ="spring", uses = {
        WorkPlaceMapper.class,
        EmployeeMapper.class
})
public interface PhoneMapper extends EntityMapper<PhoneDTO, Phone>{

    @Mapping(source = "employeeId", target = "employee")
    @Mapping(source = "workPlaceId", target = "workPlace")
    Phone toEntity(PhoneDTO phoneDTO);

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(source = "workPlace.id", target = "workPlaceId")
    @Mapping(source = "employee.name", target = "employeeName")
    @Mapping(source = "workPlace.name", target = "workPlaceName")
    @Mapping(target = "workPlace", ignore = true)
    @Mapping(target = "employee", ignore = true)
    PhoneDTO toDto(Phone phone);

    default Phone fromId(UUID id){
        if (id == null){
            return null;
        }
        Phone phone = new Phone();
        phone.setId(id);
        return phone;
    }
}
