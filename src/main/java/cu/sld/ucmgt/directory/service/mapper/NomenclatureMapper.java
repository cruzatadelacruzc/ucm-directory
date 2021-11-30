package cu.sld.ucmgt.directory.service.mapper;

import cu.sld.ucmgt.directory.domain.Nomenclature;
import cu.sld.ucmgt.directory.service.dto.NomenclatureDTO;
import org.mapstruct.Mapper;

import java.util.UUID;

/**
 * Mapper for the entity {@link Nomenclature} and its DTO {@link NomenclatureDTO}.
 */
@Mapper(componentModel = "spring")
public interface NomenclatureMapper extends EntityMapper<NomenclatureDTO, Nomenclature> {

    default Nomenclature fromId(UUID uid){
        if (uid == null){
            return null;
        }
        Nomenclature nomenclature = new Nomenclature();
        nomenclature.setId(uid);
        return nomenclature;
    }
}
