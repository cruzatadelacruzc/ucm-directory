package cu.sld.ucmgt.directory.service.dto;

import cu.sld.ucmgt.directory.TestUtil;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class NomenclatureDTOTest {

    @Test
    public void dtoEqualsVerifier() throws Exception {
        
        TestUtil.equalsVerifier(NomenclatureDTO.class);
        NomenclatureDTO nomenclatureDTO1 = new NomenclatureDTO();
        nomenclatureDTO1.setId(UUID.randomUUID());
        NomenclatureDTO nomenclatureDTO2 = new NomenclatureDTO();
        nomenclatureDTO2.setId(nomenclatureDTO1.getId());
        assertThat(nomenclatureDTO1).isEqualTo(nomenclatureDTO2);
        nomenclatureDTO2.setId(UUID.randomUUID());
        assertThat(nomenclatureDTO1).isNotEqualTo(nomenclatureDTO2);
        nomenclatureDTO1.setId(null);
        assertThat(nomenclatureDTO1).isNotEqualTo(nomenclatureDTO2);
    }
}
