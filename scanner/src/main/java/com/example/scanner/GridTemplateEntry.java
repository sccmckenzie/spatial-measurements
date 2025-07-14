package com.example.scanner;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

@Entity
@Getter
@Table(schema = "config", name = "grid")
@Immutable
public class GridTemplateEntry {

    @Id
    private Integer gridId;

    private Integer x;
    private Integer y;

}
