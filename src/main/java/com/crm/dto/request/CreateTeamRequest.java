package com.crm.dto.request;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CreateTeamRequest {
    private Long teamIdFk;
    private Long teamMemberIdFk;
    private Long roleIdFk;
}
