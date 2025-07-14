package com.example.scanner;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class Runner implements CommandLineRunner {
    private final MeasurementBuilder measurementBuilder;
    private final MeasurementRepository measurementRepository;

    @Override
    public void run(String... args) {
        measurementBuilder.setScanId(1L);
        measurementBuilder.setX(1);
        measurementBuilder.setY(2);
        measurementBuilder.initializeMeasurement();
        measurementBuilder.generateMeasurementValue();
        Measurement measurement = measurementBuilder.getMeasurement();
        measurementRepository.save(measurement);
    }
}
