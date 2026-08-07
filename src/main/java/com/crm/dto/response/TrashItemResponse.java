package com.crm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrashItemResponse {
    private String id;
    private String itemType;   // e.g., "Lead", "Contact", "Opportunity", "Organization", "Project", "Task"
    private Long recordId;
    private String name;
    private String deletedAt;
    private String moduleKey;  // e.g., "leads", "contacts", "opportunities", "organizations", "projects", "tasks"
}
