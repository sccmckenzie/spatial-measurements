package com.example.scanner;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the multi-template grid produces scans of varying sizes.
 * If this test ever fails on "expect varying measurement counts", it likely
 * means executeScan() regressed to using a single template for all scans.
 */
@Testcontainers
@Transactional
@SpringBootTest
@ActiveProfiles("test") // disabled during tests to keep Runner from consuming scanIds and writing rows before assertions run
class ScanOrchestrationServiceIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres")
            .withInitScript("test-init.sql");


    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        r.add("spring.jpa.properties.hibernate.default_schema", () -> "raw");
        r.add("scanner.write-delay-ms-min", () -> 0);
        r.add("scanner.write-delay-ms-max", () -> 0);
    }

    @Autowired ScanOrchestrationService service;
    @Autowired MeasurementRepository measurementRepository;

    @Test
    void scansProduceVaryingMeasurementCounts() {
        int scanCount = 20;
        for (int i = 0; i < scanCount; i++) {
            service.executeScan();
        }

        Map<Long, Long> countsByScanId = measurementRepository.findAll().stream()
                .collect(Collectors.groupingBy(Measurement::getScanId, Collectors.counting()));


        assertThat(countsByScanId).hasSize(scanCount);
        assertThat(countsByScanId.values().stream().distinct().count())
                .as("expect varying measurement counts due to random template selection")  // with 10 templates, probability of all scans hitting same template is ~10^-19
                .isGreaterThan(1);


    }
}
