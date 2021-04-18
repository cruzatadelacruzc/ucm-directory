package cu.sld.ucmgt.directory.repository;

import cu.sld.ucmgt.directory.domain.Nomenclature;
import cu.sld.ucmgt.directory.domain.NomenclatureType;

import java.util.Optional;
import java.util.UUID;

public interface CustomNomenclatureRepository {

    Optional<Nomenclature> findNomenclatureWithUniqueNameAndUniqueDiscriminator(String name, NomenclatureType discriminator, UUID id, boolean isParentDistrict);
}
