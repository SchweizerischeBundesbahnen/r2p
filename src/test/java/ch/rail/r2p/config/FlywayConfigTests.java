package ch.rail.r2p.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "r2p.flyway-clean-on-validation-error=true",
        "spring.datasource.url=jdbc:h2:mem:flyway-test-db;MODE=PostgreSQL;DB_CLOSE_DELAY=-1" // unique database for this test
})
@ExtendWith(OutputCaptureExtension.class)
class FlywayConfigTests {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void should_apply_migrations(CapturedOutput output) {
        // Given: the application is started with the custom Flyway migration strategy

        // Then: the expected tables should exist
        assertThat(output).contains("Successfully applied", "migrations to schema", "now at version");
        assertTableExists("TRAIN_VERSION");
        assertTableExists("BRANCH");
        assertTableExists("TRAIN_PARTNER");
        assertTableExists("CHANGELOG");
    }

    private void assertTableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?",
                Integer.class,
                tableName
        );
        assertThat(count)
                .withFailMessage("Table %s does not exist - migration not applied correctly!", tableName)
                .isEqualTo(1);
    }
}



