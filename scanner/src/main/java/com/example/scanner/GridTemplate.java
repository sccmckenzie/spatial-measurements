package com.example.scanner;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Getter
public class GridTemplate {
    private final List<GridTemplateEntry> grid;

    public GridTemplate(GridTemplateEntryRepository gridTemplateEntryRepository) {
        grid = gridTemplateEntryRepository.findAll();
    }
}
