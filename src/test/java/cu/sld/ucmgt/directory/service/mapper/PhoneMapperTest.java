package cu.sld.ucmgt.directory.service.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class PhoneMapperTest {

    private PhoneMapper mapper;

    @BeforeEach
    public void setUp() {
        mapper = new PhoneMapperImpl();
    }

    @Test
    public void testEntityFromId() {
        UUID uid = UUID.randomUUID();
        assertThat(mapper.fromId(uid).getId()).isEqualTo(uid);
        assertThat(mapper.fromId(null)).isNull();
    }
}
