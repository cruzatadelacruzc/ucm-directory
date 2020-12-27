package cu.sld.ucmgt.directory.service.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkPlaceMapperTest {
    private WorkPlaceMapper mapper;

    @BeforeEach
    public void setUp() {
        mapper = new WorkPlaceMapperImpl();
    }

    @Test
    public void testEntityFromId() {
        UUID uid = UUID.randomUUID();
        assertThat(mapper.fromId(uid).getId()).isEqualTo(uid);
        assertThat(mapper.fromId(null)).isNull();
    }
}
