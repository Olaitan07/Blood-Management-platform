package com.blood;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BloodPlatformModulithTests {

    @Test
    void contextLoads() {
    }

    @Test
    void moduleStructureIsValid() {
        ApplicationModules modules = ApplicationModules.of(BloodPlatformApplication.class);
        modules.verify();
    }
}
