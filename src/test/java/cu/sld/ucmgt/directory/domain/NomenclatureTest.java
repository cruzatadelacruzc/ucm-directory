package cu.sld.ucmgt.directory.domain;

import cu.sld.ucmgt.directory.TestUtil;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class NomenclatureTest {

    @Test
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Nomenclature.class);
        Nomenclature nomenclature1 = new Nomenclature();
        nomenclature1.setId(UUID.randomUUID());
        Nomenclature nomenclature2 = new Nomenclature();
        nomenclature2.setId(nomenclature1.getId());
        assertThat(nomenclature1).isEqualTo(nomenclature2);
        nomenclature2.setId(UUID.randomUUID());
        assertThat(nomenclature1).isNotEqualTo(nomenclature2);
        nomenclature1.setId(null);
        assertThat(nomenclature1).isNotEqualTo(nomenclature2);
    }
}
