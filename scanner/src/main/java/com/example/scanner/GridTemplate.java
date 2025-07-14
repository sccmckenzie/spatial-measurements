package com.example.scanner;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Getter
public class GridTemplate {
    private final List<GridTemplateEntry> GRID;

    public GridTemplate(GridTemplateEntryRepository gridTemplateEntryRepository) {
        GRID = gridTemplateEntryRepository.findAll();
    }
}
