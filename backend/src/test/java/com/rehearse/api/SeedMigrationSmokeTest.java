package com.rehearse.api;

import com.rehearse.api.global.support.AbstractMySqlContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed"
)
@ActiveProfiles("test")
class SeedMigrationSmokeTest extends AbstractMySqlContainerTest {

    @Test
    void seedFlywayAppliesWithoutError() {
        // context load = migration + seed Flyway 모두 오류 없이 적용됨
    }
}
