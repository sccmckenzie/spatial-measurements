package com.example.scanner;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Scope("prototype")
@Getter
@Setter // eventually this won't be needed once we pass gridtemplateentry
public class MeasurementBuilder {
    private Measurement measurement;
    private GridTemplateEntry gridTemplateEntry;
    private long scanId;

    public void initializeMeasurement() {
        int x = gridTemplateEntry.getX();
        int y = gridTemplateEntry.getY();
        this.measurement = new Measurement(scanId, x, y);
    }

    public void generateMeasurementValue() {
        var value = ThreadLocalRandom.current().nextDouble(-1, 1);
        measurement.setMeasurementValue(value);
    }
}
