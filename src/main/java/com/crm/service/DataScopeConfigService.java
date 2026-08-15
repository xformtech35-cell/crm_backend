package com.crm.service;

import com.crm.dto.request.DataScopeConfigRequest;
import com.crm.entity.DataScopeConfig;
import com.crm.repository.DataScopeConfigRepository;
import com.crm.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DataScopeConfigService {

    private final DataScopeConfigRepository dataScopeConfigRepository;
    private final RoleRepository roleRepository;
    private final AuditLogService auditLogService;

    private static final List<String> ALL_MODULES = List.of(
        "DASHBOARD", "ACTIVITIES", "EMAILS", "CALENDAR", "ATTENDANCE",
        "LEADS", "NEGOTIATIONS", "LEAD_STATUS", "CONTACTS", "ORGANIZATIONS", "OPPORTUNITIES",
        "PROJECTS", "TASKS", "TEAMS", "TEAM_LEADS", "TEAM_MEMBERS",
        "ANALYTICS", "REPORTS", "AUTOMATION",
        "ROLES", "INTEGRATIONS", "DATA_ACCESS", "SETTINGS", "TRASH"
    );

    public List<DataScopeConfig> getAllConfigs(Long companyAdminId) {
        if (companyAdminId == null) {
            return dataScopeConfigRepository.findAll();
        }
        return dataScopeConfigRepository.findByCompanyAdminIdFk(companyAdminId);
    }

    @Transactional
    public DataScopeConfig saveConfig(DataScopeConfigRequest req, Long companyAdminId, Long actorUserId) {
        String moduleName = req.getModuleName().toUpperCase();
        String scopeMode  = req.getScopeMode().toUpperCase();

        Optional<DataScopeConfig> existingOpt;
        if (req.getUserIdFk() != null) {
            existingOpt = companyAdminId == null
                ? dataScopeConfigRepository.findByUserIdFkAndModuleName(req.getUserIdFk(), moduleName)
                : dataScopeConfigRepository.findByCompanyAdminIdFkAndUserIdFkAndModuleName(companyAdminId, req.getUserIdFk(), moduleName);
        } else if (req.getRoleIdFk() != null) {
            existingOpt = companyAdminId == null
                ? dataScopeConfigRepository.findByRoleIdFkAndModuleName(req.getRoleIdFk(), moduleName)
                : dataScopeConfigRepository.findByCompanyAdminIdFkAndRoleIdFkAndModuleName(companyAdminId, req.getRoleIdFk(), moduleName);
        } else {
            throw new IllegalArgumentException("Either roleIdFk or userIdFk must be specified");
        }

        String oldScope = existingOpt.map(DataScopeConfig::getScopeMode).orElse(null);
        DataScopeConfig config;
        if (existingOpt.isPresent()) {
            config = existingOpt.get();
            config.setScopeMode(scopeMode);
        } else {
            config = DataScopeConfig.builder()
                    .roleIdFk(req.getRoleIdFk())
                    .userIdFk(req.getUserIdFk())
                    .companyAdminIdFk(companyAdminId)
                    .moduleName(moduleName)
                    .scopeMode(scopeMode)
                    .build();
        }
        DataScopeConfig saved = dataScopeConfigRepository.save(config);

        if (actorUserId != null && !scopeMode.equals(oldScope)) {
            auditLogService.log(actorUserId, "SCOPE_CHANGE", "DataScopeConfig",
                saved.getConfigId(), oldScope, scopeMode, companyAdminId);
        }
        return saved;
    }

    @Transactional
    public DataScopeConfig saveConfig(DataScopeConfigRequest req, Long companyAdminId) {
        return saveConfig(req, companyAdminId, null);
    }

    @Transactional
    public List<DataScopeConfig> saveBatchConfigs(List<DataScopeConfigRequest> requests, Long companyAdminId, Long actorUserId) {
        return requests.stream().map(req -> saveConfig(req, companyAdminId, actorUserId)).toList();
    }

    @Transactional
    public List<DataScopeConfig> saveBatchConfigs(List<DataScopeConfigRequest> requests, Long companyAdminId) {
        return saveBatchConfigs(requests, companyAdminId, null);
    }

    @Transactional
    public List<DataScopeConfig> saveAllModulesForRole(Long roleId, String scopeMode, Long companyAdminId, Long actorUserId) {
        List<DataScopeConfigRequest> requests = ALL_MODULES.stream()
                .map(module -> DataScopeConfigRequest.builder()
                        .roleIdFk(roleId).moduleName(module).scopeMode(scopeMode).build())
                .toList();
        return saveBatchConfigs(requests, companyAdminId, actorUserId);
    }
}