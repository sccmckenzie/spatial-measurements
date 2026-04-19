package com.example.scanner;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GridTemplateEntryRepository extends JpaRepository<GridTemplateEntry, Long> {
    @Query("select distinct g.templateId from GridTemplateEntry g")
    List<Integer> findDistinctTemplateIds();

    List<GridTemplateEntry> findByTemplateId(Integer templateId);
}
