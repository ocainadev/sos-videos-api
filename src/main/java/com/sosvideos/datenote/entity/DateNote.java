package com.sosvideos.datenote.entity;

import com.sosvideos.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "date_notes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DateNote extends BaseEntity {

    @Column(nullable = false, unique = true)
    private LocalDate date;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
}
