package com.crm.repository;

import com.crm.entity.DataScopeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataScopeConfigRepository extends JpaRepository<DataScopeConfig, Long> {

    List<DataScopeConfig> findByCompanyAdminIdFk(Long companyAdminIdFk);

    List<DataScopeConfig> findByRoleIdFk(Long roleIdFk);

    List<DataScopeConfig> findByUserIdFk(Long userIdFk);

    Optional<DataScopeConfig> findByRoleIdFkAndModuleName(Long roleIdFk, String moduleName);

    Optional<DataScopeConfig> findByUserIdFkAndModuleName(Long userIdFk, String moduleName);

    Optional<DataScopeConfig> findByCompanyAdminIdFkAndRoleIdFkAndModuleName(Long companyAdminIdFk, Long roleIdFk, String moduleName);

    Optional<DataScopeConfig> findByCompanyAdminIdFkAndUserIdFkAndModuleName(Long companyAdminIdFk, Long userIdFk, String moduleName);
}
