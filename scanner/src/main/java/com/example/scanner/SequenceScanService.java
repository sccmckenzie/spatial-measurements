package com.example.scanner;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SequenceScanService {
    
    private final JdbcTemplate jdbcTemplate;
    
    public Long getNextScanId() {
        String sql = "SELECT nextval('raw_measurements.scan_id_sequence')";
        return jdbcTemplate.queryForObject(sql, Long.class);
    }
} 