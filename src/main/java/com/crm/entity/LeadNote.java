package com.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;


@Entity
@Table(name = "crm_xformsales_lead_note")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeadNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lead_note_id")
    private Long leadNoteId;

    @Column(name = "lead_id_fk")
    private Long leadIdFk;

    @Column(name = "note_text", columnDefinition = "TEXT")
    private String noteText;

    @Column(name = "note_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    private LocalDateTime noteDate;


    @Column(name = "user_id_fk")
    private Long userIdFk;
}
