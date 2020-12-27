package cu.sld.ucmgt.directory.domain;

import cu.sld.ucmgt.directory.TestUtil;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkPlaceTest {
    
    @Test
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(WorkPlace.class);
        WorkPlace workPlace1 = new WorkPlace();
        workPlace1.setId(UUID.randomUUID());
        WorkPlace workPlace2 = new WorkPlace();
        workPlace2.setId(workPlace1.getId());
        assertThat(workPlace1).isEqualTo(workPlace2);
        workPlace2.setId(UUID.randomUUID());
        assertThat(workPlace1).isNotEqualTo(workPlace2);
        workPlace1.setId(null);
        assertThat(workPlace1).isNotEqualTo(workPlace2);
    }
}
