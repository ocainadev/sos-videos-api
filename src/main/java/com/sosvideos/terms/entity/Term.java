package com.sosvideos.terms.entity;

import com.sosvideos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "terms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Term extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 50)
    private String version;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = false;
}
