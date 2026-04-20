package com.example.scanner;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ScanOrchestrationService {
    private final MeasurementRepository measurementRepository;
    private final GridTemplateEntryRepository gridTemplateEntryRepository;
    private final SequenceScanService sequenceScanService;
    private final ScannerProperties properties;
    private static final Logger logger = LoggerFactory.getLogger(Runner.class);
    private List<Integer> templateIds;

    @PostConstruct
    void loadTemplateIds() {
        this.templateIds = gridTemplateEntryRepository.findDistinctTemplateIds();
        logger.info("Loaded {} template ids: {}", templateIds.size(), templateIds);
    }


    public void executeScan() {
        int templateId = templateIds.get(ThreadLocalRandom.current().nextInt(templateIds.size()));
        List<GridTemplateEntry> gridTemplateEntries = gridTemplateEntryRepository.findByTemplateId(templateId);

        long scanId = sequenceScanService.getNextScanId();

        MeasurementBuilder measurementBuilder = new MeasurementBuilder();
        measurementBuilder.setScanId(scanId);
        measurementBuilder.setMeasurementMin(properties.measurementMin());
        measurementBuilder.setMeasurementMax(properties.measurementMax());
        int i = 0;

        logger.info("scanId: {}, starting", scanId);


        for (GridTemplateEntry gridTemplateEntry : gridTemplateEntries) {
            i++;
            measurementBuilder.setGridTemplateEntry(gridTemplateEntry);
            measurementBuilder.initializeMeasurement();
            measurementBuilder.generateMeasurementValue();
            Measurement measurement = measurementBuilder.getMeasurement();
            sleepVariableWriteDuration();
            measurementRepository.save(measurement);
        }

        // logger.info("scanId: {}, i: {}", scanId, i);
    }

    // Intentional per-write pause so concurrent scans produce interleaved modified_at timestamps — core to the case study's mid-scan partial-visibility problem. Do not remove.
    private void sleepVariableWriteDuration() {
        if (properties.writeDelayMsMax() <= 0) {
            return;
        }
        long delay = ThreadLocalRandom.current().nextLong(properties.writeDelayMsMin(), properties.writeDelayMsMax() + 1);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Scan interrupted during write-duration sleep", e);
        }
    }
} 