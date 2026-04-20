package com.example.scanner;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("scanner")
public record ScannerProperties(
        @DefaultValue("1") int poolSize,
        @DefaultValue("10") int count,
        @DefaultValue("0") long writeDelayMsMin,
        @DefaultValue("0") long writeDelayMsMax,
        @DefaultValue("-1.0") double measurementMin,
        @DefaultValue("1.0") double measurementMax
) {}
