package ch.rail.r2p.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway configuration that validates the schema and resets/rebuilds the database
 * if validation fails. This is useful for deployed instances with file-based H2
 * databases when migrations change.
 */
@Configuration
@Slf4j
public class FlywayConfig {

    /**
     * Migration strategy that validates the schema and resets the database
     * if validation fails. This ensures that schema changes in V1__create.sql
     * don't break existing deployments.
     */
    @Bean
    @ConditionalOnBooleanProperty(prefix = "r2p", name = "flyway-clean-on-validation-error")
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            try {
                var result = flyway.validateWithResult();
                if (result.validationSuccessful) {
                    log.info("Flyway validated the schema: OK ({} migrations)", result.validateCount);
                } else {
                    log.warn("Flyway validated the schema: NOK");
                    log.warn("Flyway found the following errors: {}", result.getAllErrorMessages());
                    log.warn("Attempting to reset and rebuild the database...");
                    resetAndRebuild(flyway);
                }
            } catch (Exception e) {
                log.error("Flyway validation failed with exception: {}", e.getMessage(), e);
                log.warn("Attempting to reset and rebuild the database...");
                resetAndRebuild(flyway);
            }
        };
    }

    /**
     * Resets the flyway metadata table and rebuilds the database schema.
     * This is safe for development/deployment scenarios where data loss is acceptable.
     */
    private void resetAndRebuild(Flyway flyway) {
        try {
            flyway.clean();
            log.info("Database cleaned successfully");
            flyway.migrate();
            log.info("Database rebuilt successfully");
        } catch (Exception e) {
            log.error("Failed to reset and rebuild database: {}", e.getMessage(), e);
            throw new RuntimeException("Flyway migration recovery failed", e);
        }
    }
}
