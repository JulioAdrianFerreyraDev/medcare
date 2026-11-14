package com.medcare.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Automatically repairs Flyway schema history checksums/descriptions (e.g. after renaming
 * migration filenames to domain-driven names) before executing pending migrations.
 */
@Configuration
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            log.info("Running Flyway repair to synchronize migration checksums and descriptions...");
            flyway.repair();
            flyway.migrate();
        };
    }
}
