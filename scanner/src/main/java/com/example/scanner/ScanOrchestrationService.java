package com.example.scanner;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScanOrchestrationService {
    private final MeasurementRepository measurementRepository;
    private final GridTemplateEntryRepository gridTemplateEntryRepository;
    private final SequenceScanService sequenceScanService;
    private static final Logger logger = LoggerFactory.getLogger(Runner.class);

    public void executeScan() {
        List<GridTemplateEntry> gridTemplateEntries = gridTemplateEntryRepository.findAll();
        long scanId = sequenceScanService.getNextScanId();

        MeasurementBuilder measurementBuilder = new MeasurementBuilder();
        measurementBuilder.setScanId(scanId);
        int i = 0;

        logger.info("scanId: {}, starting", scanId);


        for (GridTemplateEntry gridTemplateEntry : gridTemplateEntries) {
            i++;
            measurementBuilder.setGridTemplateEntry(gridTemplateEntry);
            measurementBuilder.initializeMeasurement();
            measurementBuilder.generateMeasurementValue();
            Measurement measurement = measurementBuilder.getMeasurement();
            measurementRepository.save(measurement);
        }

        // logger.info("scanId: {}, i: {}", scanId, i);
    }
} 