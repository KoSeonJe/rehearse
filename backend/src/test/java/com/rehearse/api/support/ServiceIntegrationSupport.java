package com.rehearse.api.support;

import com.rehearse.api.global.support.AbstractMySqlContainerTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
public abstract class ServiceIntegrationSupport extends AbstractMySqlContainerTest {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void truncateDatabase() {
        truncateAllTables();
    }

    @AfterEach
    void cleanDatabase() {
        truncateAllTables();
    }

    private void truncateAllTables() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        try {
            List<String> tables = jdbcTemplate.queryForList("""
                    SELECT table_name
                    FROM information_schema.tables
                    WHERE table_schema = DATABASE()
                      AND table_type = 'BASE TABLE'
                      AND table_name <> 'flyway_schema_history'
                    """, String.class);
            tables.forEach(table -> jdbcTemplate.execute("TRUNCATE TABLE `" + table + "`"));
        } finally {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }
}
