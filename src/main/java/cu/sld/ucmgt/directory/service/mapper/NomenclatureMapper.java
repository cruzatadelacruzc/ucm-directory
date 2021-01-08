package cu.sld.ucmgt.directory.service.mapper;

import org.mapstruct.Mapper;
import cu.sld.ucmgt.directory.domain.Nomenclature;
import cu.sld.ucmgt.directory.service.dto.NomenclatureDTO;
import org.mapstruct.Mapping;

import java.util.UUID;

/**
 * Mapper for the entity {@link Nomenclature} and its DTO {@link NomenclatureDTO}.
 */
@Mapper(componentModel = "spring")
public interface NomenclatureMapper extends EntityMapper<NomenclatureDTO, Nomenclature> {

    @Mapping(target = "peopleSpecialty", ignore = true)
    @Mapping(target = "studentCareer", ignore = true)
    @Mapping(target = "studentsKind", ignore = true)
    @Mapping(target = "employeesCategory", ignore = true)
    @Mapping(target = "employeesScientificDegree", ignore = true)
    @Mapping(target = "employeesTeachingCategory", ignore = true)
    @Mapping(target = "employeesCharge", ignore = true)
    Nomenclature toEntity(NomenclatureDTO projectDto);

    default Nomenclature fromId(UUID uid){
        if (uid == null){
            return null;
        }
        Nomenclature nomenclature = new Nomenclature();
        nomenclature.setId(uid);
        return nomenclature;
    }
}
