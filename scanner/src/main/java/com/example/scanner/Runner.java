package com.example.scanner;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@RequiredArgsConstructor
public class Runner implements CommandLineRunner {
    private final MeasurementBuilder measurementBuilder;
    private final MeasurementRepository measurementRepository;
    private final GridTemplateEntryRepository gridTemplateEntryRepository;
    private final SequenceScanService sequenceScanService;

    @Override
    public void run(String... args) {
        List<GridTemplateEntry> gridTemplateEntries = gridTemplateEntryRepository.findAll();
        long scanId = sequenceScanService.getNextScanId();
        for (GridTemplateEntry gridTemplateEntry : gridTemplateEntries) {
            measurementBuilder.setScanId(scanId);
            measurementBuilder.setGridTemplateEntry(gridTemplateEntry);
            measurementBuilder.initializeMeasurement();
            measurementBuilder.generateMeasurementValue();
            Measurement measurement = measurementBuilder.getMeasurement();
            measurementRepository.save(measurement);
        }
    }
}
