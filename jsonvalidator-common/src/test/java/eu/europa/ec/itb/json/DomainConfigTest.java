package eu.europa.ec.itb.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DomainConfigTest {

    @Test
    void testNewLabelConfig() {
        var config = new DomainConfig();
        assertNotNull(config);
    }

}
