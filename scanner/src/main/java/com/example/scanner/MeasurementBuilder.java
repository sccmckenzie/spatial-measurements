package com.example.scanner;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import java.util.concurrent.ThreadLocalRandom;

@Scope("prototype")
@Getter
@Setter
public class MeasurementBuilder {
    private Measurement measurement;
    private GridTemplateEntry gridTemplateEntry;
    private long scanId;
    private double measurementMin;
    private double measurementMax;

    public void initializeMeasurement() {
        int x = gridTemplateEntry.getX();
        int y = gridTemplateEntry.getY();
        this.measurement = new Measurement(this.scanId, x, y);
    }

    public void generateMeasurementValue() {
        var value = ThreadLocalRandom.current().nextDouble(measurementMin, measurementMax);
        measurement.setMeasurementValue(value);
    }
}
