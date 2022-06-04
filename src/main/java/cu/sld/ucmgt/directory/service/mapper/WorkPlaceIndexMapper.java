package cu.sld.ucmgt.directory.service.mapper;

import cu.sld.ucmgt.directory.domain.Phone;
import cu.sld.ucmgt.directory.domain.WorkPlace;
import cu.sld.ucmgt.directory.domain.elasticsearch.PhoneIndex;
import cu.sld.ucmgt.directory.domain.elasticsearch.WorkPlaceIndex;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.HashSet;
import java.util.Set;

/**
 * Mapper for the index {@link WorkPlaceIndex} and its entity {@link WorkPlace}.
 */
@Mapper(componentModel = "spring")
public abstract class WorkPlaceIndexMapper {

    private final EmployeeIndexMapper employeeIndexMapper = Mappers.getMapper(EmployeeIndexMapper.class);


    public WorkPlaceIndex toIndex(WorkPlace workPlace) {
        if ( workPlace == null ) {
            return null;
        }

        WorkPlaceIndex workPlaceIndex = new WorkPlaceIndex();

        workPlaceIndex.setId( workPlace.getId() );
        workPlaceIndex.setName( workPlace.getName() );
        workPlaceIndex.setEmail(workPlace.getEmail());
        workPlaceIndex.setAvatarUrl(workPlace.getAvatarUrl());
        workPlaceIndex.setDescription(workPlace.getDescription());
        workPlaceIndex.setEmployees( employeeIndexMapper.toIndices( workPlace.getEmployees() ) );
        workPlaceIndex.setPhones( mapPhonesToPhoneIndices( workPlace.getPhones() ) );

        return workPlaceIndex;
    }

    public Set<PhoneIndex> mapPhonesToPhoneIndices(Set<Phone> phones) {
        if ( phones == null ) {
            return null;
        }

        Set<PhoneIndex> set = new HashSet<>(Math.max((int) (phones.size() / .75f) + 1, 16));
        for ( Phone phone : phones ) {
            PhoneIndex phoneIndex = new PhoneIndex();
            phoneIndex.setId(phone.getId());
            phoneIndex.setDescription(phone.getDescription());
            phoneIndex.setNumber(phone.getNumber());
            set.add( phoneIndex );
        }

        return set;
    }

    public abstract Set<WorkPlaceIndex> toIndices(Set<WorkPlace> workPlaces);
}
