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

    public List<DataScopeConfig> getAllConfigs(Long companyAdminId) {
        if (companyAdminId == null) {
            return dataScopeConfigRepository.findAll();
        }
        return dataScopeConfigRepository.findByCompanyAdminIdFk(companyAdminId);
    }

    @Transactional
    public DataScopeConfig saveConfig(DataScopeConfigRequest req, Long companyAdminId) {
        String moduleName = req.getModuleName().toUpperCase();
        String scopeMode = req.getScopeMode().toUpperCase();

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

        return dataScopeConfigRepository.save(config);
    }

    @Transactional
    public List<DataScopeConfig> saveBatchConfigs(List<DataScopeConfigRequest> requests, Long companyAdminId) {
        return requests.stream()
                .map(req -> saveConfig(req, companyAdminId))
                .toList();
    }
}
