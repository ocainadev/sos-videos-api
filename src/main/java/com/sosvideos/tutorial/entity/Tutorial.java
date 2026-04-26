package com.sosvideos.tutorial.entity;

import com.sosvideos.common.BaseEntity;
import com.sosvideos.tutorial.enums.TutorialContentType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tutorials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tutorial extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TutorialContentType contentType;

    @Column(length = 1000)
    private String url;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
