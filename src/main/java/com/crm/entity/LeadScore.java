package com.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;


@Entity
@Table(name = "crm_xformsales_lead_score")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeadScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "score_id")
    private Long scoreId;

    @Column(name = "lead_id_fk", nullable = false, unique = true)
    private Long leadIdFk;

    /** Normalized score 0–100 */
    @Column(name = "score", nullable = false)
    private Integer score;

    /** A (80+), B (60+), C (40+), D (<40) */
    @Enumerated(EnumType.STRING)
    @Column(name = "grade", nullable = false, length = 1)
    private Grade grade;

    /** JSON array of human-readable factor strings, stored as TEXT */
    @Column(name = "top_factors", columnDefinition = "TEXT")
    private String topFactors;

    @Column(name = "calculated_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    private LocalDateTime calculatedAt;


    public enum Grade {
        A, B, C, D
    }
}
