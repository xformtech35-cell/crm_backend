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

    Optional<DataScopeConfig> findFirstByRoleIdFkAndModuleName(Long roleIdFk, String moduleName);
    default Optional<DataScopeConfig> findByRoleIdFkAndModuleName(Long roleIdFk, String moduleName) {
        return findFirstByRoleIdFkAndModuleName(roleIdFk, moduleName);
    }

    Optional<DataScopeConfig> findFirstByUserIdFkAndModuleName(Long userIdFk, String moduleName);
    default Optional<DataScopeConfig> findByUserIdFkAndModuleName(Long userIdFk, String moduleName) {
        return findFirstByUserIdFkAndModuleName(userIdFk, moduleName);
    }

    Optional<DataScopeConfig> findFirstByCompanyAdminIdFkAndRoleIdFkAndModuleName(Long companyAdminIdFk, Long roleIdFk, String moduleName);
    default Optional<DataScopeConfig> findByCompanyAdminIdFkAndRoleIdFkAndModuleName(Long companyAdminIdFk, Long roleIdFk, String moduleName) {
        return findFirstByCompanyAdminIdFkAndRoleIdFkAndModuleName(companyAdminIdFk, roleIdFk, moduleName);
    }

    Optional<DataScopeConfig> findFirstByCompanyAdminIdFkAndUserIdFkAndModuleName(Long companyAdminIdFk, Long userIdFk, String moduleName);
    default Optional<DataScopeConfig> findByCompanyAdminIdFkAndUserIdFkAndModuleName(Long companyAdminIdFk, Long userIdFk, String moduleName) {
        return findFirstByCompanyAdminIdFkAndUserIdFkAndModuleName(companyAdminIdFk, userIdFk, moduleName);
    }
}
