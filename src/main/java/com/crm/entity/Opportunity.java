package com.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "crm_xformsales_opportunity")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@org.hibernate.annotations.SQLDelete(sql = "UPDATE crm_xformsales_opportunity SET is_deleted = true, deleted_at = NOW() WHERE opp_id = ?")
@org.hibernate.annotations.SQLRestriction("(is_deleted = false OR is_deleted IS NULL)")
public class Opportunity {

    @Builder.Default
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "opp_id")
    private Long oppId;

    @Column(name = "opp_name")
    private String oppName;

    @Column(name = "opp_title")
    private String oppTitle;

    @Column(name = "opp_status")
    private String oppStatus;

    @Column(name = "opp_amount", precision = 15, scale = 2)
    private BigDecimal oppAmount;

    @Column(name = "opp_forcast_close_date")
    private LocalDate oppForcastCloseDate;

    @Column(name = "opp_actual_close_date")
    private LocalDate oppActualCloseDate;

    @Column(name = "opp_description", columnDefinition = "TEXT")
    private String oppDescription;

    @Column(name = "opp_doc")
    private String oppDoc;

    @Column(name = "user_id_fk")
    private Long userIdFk;

    @Column(name = "lead_id_fk")
    private Long leadIdFk;
}
