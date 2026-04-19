package com.example.scanner;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

@Entity
@Getter
@Setter
@Table(schema = "config", name = "grid")
@Immutable
public class GridTemplateEntry {

    @Id
    private Integer id;

    private Integer templateId;

    private Integer x;
    private Integer y;

}
