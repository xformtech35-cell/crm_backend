package com.crm.service;

import com.crm.entity.AuditLog;
import com.crm.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public void log(Long actorUserId, String actionType, String entityType,
                    Long entityId, Object oldValue, Object newValue, Long companyAdminId) {
        try {
            AuditLog entry = AuditLog.builder()
                    .actorUserId(actorUserId)
                    .actionType(actionType)
                    .entityType(entityType)
                    .entityId(entityId)
                    .oldValue(oldValue != null ? objectMapper.writeValueAsString(oldValue) : null)
                    .newValue(newValue != null ? objectMapper.writeValueAsString(newValue) : null)
                    .timestamp(LocalDateTime.now())
                    .companyAdminIdFk(companyAdminId)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("Audit log write failed (non-fatal): action={}, entity={}/{}, error={}",
                    actionType, entityType, entityId, e.getMessage());
        }
    }

    public Page<AuditLog> getAuditLog(Long companyAdminId, Pageable pageable) {
        if (companyAdminId == null) {
            return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
        }
        return auditLogRepository.findByCompanyAdminIdFkOrderByTimestampDesc(companyAdminId, pageable);
    }
}