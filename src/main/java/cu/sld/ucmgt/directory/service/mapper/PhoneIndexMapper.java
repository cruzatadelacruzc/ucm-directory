package cu.sld.ucmgt.directory.service.mapper;

import cu.sld.ucmgt.directory.domain.Phone;
import cu.sld.ucmgt.directory.domain.elasticsearch.PhoneIndex;
import org.mapstruct.Context;
import org.mapstruct.Mapper;

/**
 * Mapper for the index {@link PhoneIndex} and its entity {@link Phone}.
 */
@Mapper(componentModel = "spring", uses = {EmployeeIndexMapper.class, WorkPlaceIndexMapper.class})
public interface PhoneIndexMapper extends IndexMapper<PhoneIndex, Phone> {
}
