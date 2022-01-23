package cu.sld.ucmgt.directory.service.mapper;

import cu.sld.ucmgt.directory.domain.Phone;
import cu.sld.ucmgt.directory.domain.WorkPlace;
import cu.sld.ucmgt.directory.domain.elasticsearch.PhoneIndex;
import cu.sld.ucmgt.directory.domain.elasticsearch.WorkPlaceIndex;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.Collections;
import java.util.Optional;

/**
 * Mapper for the index {@link PhoneIndex} and its entity {@link Phone}.
 */
@Mapper(componentModel = "spring", uses = {EmployeeIndexMapper.class})
public interface PhoneIndexMapper extends IndexMapper<PhoneIndex, Phone> {
    WorkPlaceIndexMapper WORK_PLACE_INDEX_MAPPER = Mappers.getMapper(WorkPlaceIndexMapper.class);

    default WorkPlaceIndex workPlaceToWorkPlaceIndex(WorkPlace workPlace) {
        return Optional.ofNullable(workPlace)
                .map(WORK_PLACE_INDEX_MAPPER::toIndex)
                .map(this::removePhonesAndWorkPLaceOfEmployees)
                .orElse( null);
    }

    default WorkPlaceIndex removePhonesAndWorkPLaceOfEmployees(WorkPlaceIndex workPlaceIndex) {
        workPlaceIndex.getEmployees().forEach(employeeIndex -> employeeIndex.setWorkPlace(null));
        workPlaceIndex.setPhones(Collections.emptySet());
        return workPlaceIndex;
    }
}
