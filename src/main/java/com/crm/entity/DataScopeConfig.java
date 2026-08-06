package com.crm.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "crm_data_scope_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataScopeConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id")
    private Long configId;

    @Column(name = "role_id_fk")
    private Long roleIdFk;

    @Column(name = "user_id_fk")
    private Long userIdFk;

    @Column(name = "company_admin_id_fk")
    private Long companyAdminIdFk;

    @Column(name = "module_name", length = 50, nullable = false)
    private String moduleName; // e.g., "LEADS", "TASKS", "OPPORTUNITIES", "PROJECTS", "CONTACTS"

    @Column(name = "scope_mode", length = 30, nullable = false)
    private String scopeMode; // "ALL_DATA", "TEAM_DATA", "OWN_DATA_ONLY"
}
