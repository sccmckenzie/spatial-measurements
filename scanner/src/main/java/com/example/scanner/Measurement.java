package com.example.scanner;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;


@Entity
@Getter
@Setter
@RequiredArgsConstructor
public class Measurement {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long measurementId;
    private final Long scanId;
    private final Integer x;
    private final Integer y;
    private double measurementValue;

    public String toString() {
        return String.format(
                "[%d, %d, %d, %d, %f]",
                measurementId, scanId, x, y, measurementValue
        );
    }
}
