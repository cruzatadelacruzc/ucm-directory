package cu.sld.ucmgt.directory.service.dto;

import cu.sld.ucmgt.directory.TestUtil;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkPlaceDTOTest {

    @Test
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(WorkPlaceDTO.class);
        WorkPlaceDTO workPlaceDTO1 = new WorkPlaceDTO();
        workPlaceDTO1.setId(UUID.randomUUID());
        WorkPlaceDTO workPlaceDTO2 = new WorkPlaceDTO();
        workPlaceDTO2.setId(workPlaceDTO1.getId());
        assertThat(workPlaceDTO1).isEqualTo(workPlaceDTO2);
        workPlaceDTO2.setId(UUID.randomUUID());
        assertThat(workPlaceDTO1).isNotEqualTo(workPlaceDTO2);
        workPlaceDTO1.setId(null);
        assertThat(workPlaceDTO1).isNotEqualTo(workPlaceDTO2);
    }
}
