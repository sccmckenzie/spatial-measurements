package com.example.scanner;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class MeasurementBuilderTest {

    /**
     * Guards against state leaking between builders. Each {@code new MeasurementBuilder()}
     * must hold its own scanId/grid fields — if the class were ever refactored to use
     * static or shared mutable state, one builder's values would overwrite another's.
     */
    @Test
    @DisplayName("separate builder instances hold isolated state")
    void separateInstances_doNotShareState() {
        MeasurementBuilder a = builder(100L, -1, 1);
        a.setGridTemplateEntry(gridEntry(1, 5, 10));
        a.initializeMeasurement();
        a.generateMeasurementValue();

        MeasurementBuilder b = builder(200L, -1, 1);
        b.setGridTemplateEntry(gridEntry(2, 7, 3));
        b.initializeMeasurement();
        b.generateMeasurementValue();

        Measurement ma = a.getMeasurement();
        Measurement mb = b.getMeasurement();

        assertThat(ma.getScanId()).isEqualTo(100L);
        assertThat(ma.getX()).isEqualTo(5);
        assertThat(ma.getY()).isEqualTo(10);

        assertThat(mb.getScanId()).isEqualTo(200L);
        assertThat(mb.getX()).isEqualTo(7);
        assertThat(mb.getY()).isEqualTo(3);
    }

    /**
     * Simulates the production scenario where multiple scan threads build measurements
     * in parallel. A {@link CountDownLatch} pins all threads at the same point before
     * they call {@code initializeMeasurement()}, maximizing the chance that any shared
     * state would be observed. Each thread must end up with a measurement whose
     * scanId/x/y match only its own inputs.
     */
    @Test
    @DisplayName("concurrent builders do not cross-contaminate state")
    void concurrentBuilders_noStateCrossContamination() throws Exception {
        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        ConcurrentHashMap<Long, Measurement> results = new ConcurrentHashMap<>();
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch go = new CountDownLatch(1);

        for (int i = 0; i < threadCount; i++) {
            long scanId = i;
            int coord = i * 10;
            executor.submit(() -> {
                try {
                    MeasurementBuilder builder = builder(scanId, -1, 1);
                    builder.setGridTemplateEntry(gridEntry((int) scanId, coord, coord + 1));
                    ready.countDown();
                    go.await(5, TimeUnit.SECONDS);
                    builder.initializeMeasurement();
                    builder.generateMeasurementValue();
                    results.put(scanId, builder.getMeasurement());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        ready.await(5, TimeUnit.SECONDS);
        go.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(results).hasSize(threadCount);
        results.forEach((scanId, measurement) -> {
            assertThat(measurement.getScanId()).isEqualTo(scanId);
            assertThat(measurement.getX()).isEqualTo((int) (scanId * 10));
            assertThat(measurement.getY()).isEqualTo((int) (scanId * 10) + 1);
        });
    }

    /**
     * Verifies that the configurable {@code measurementMin}/{@code measurementMax}
     * bounds are actually used. Intentionally uses a non-default range (0, 100) so
     * that a regression to the old hardcoded (-1, 1) range would fail the assertion.
     */
    @Test
    @DisplayName("generated value respects configured min/max bounds")
    void generateMeasurementValue_withinExpectedRange() {
        MeasurementBuilder b = builder(1L, 0, 100);
        b.setGridTemplateEntry(gridEntry(1, 0, 0));
        b.initializeMeasurement();
        b.generateMeasurementValue();

        double value = b.getMeasurement().getMeasurementValue();
        assertThat(value).isBetween(0.0, 100.0);
    }

    private static MeasurementBuilder builder(long scanId, double min, double max) {
        MeasurementBuilder b = new MeasurementBuilder();
        b.setScanId(scanId);
        b.setMeasurementMin(min);
        b.setMeasurementMax(max);
        return b;
    }

    private static GridTemplateEntry gridEntry(int gridId, int x, int y) {
        GridTemplateEntry entry = new GridTemplateEntry();
        entry.setGridId(gridId);
        entry.setX(x);
        entry.setY(y);
        return entry;
    }
}