package cu.sld.ucmgt.directory.service.mapper;

import cu.sld.ucmgt.directory.domain.WorkPlace;
import cu.sld.ucmgt.directory.service.dto.WorkPlaceDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

/**
 * Mapper for the entity {@link WorkPlace} and its DTO {@link WorkPlaceDTO}.
 */
@Mapper(componentModel = "spring", uses = {EmployeeMapper.class})
public interface WorkPlaceMapper extends EntityMapper<WorkPlaceDTO, WorkPlace> {

    @Mapping(target = "phones", ignore = true)
    @Mapping(target = "employees", ignore = true)
    WorkPlace toEntity(WorkPlaceDTO dto);

    @Mapping(target = "employees", ignore = true)
    WorkPlaceDTO toDto(WorkPlace entity);

    default WorkPlace fromId(UUID uid) {
        if (uid == null) {
            return null;
        }
        WorkPlace workPlace = new WorkPlace();
        workPlace.setId(uid);
        return workPlace;
    }
}
