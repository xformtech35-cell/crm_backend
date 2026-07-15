package com.crm.repository;

import com.crm.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    List<Permission> findByRoleIdFk(Long roleIdFk);
    boolean existsByRoleIdFkAndGrpPerm(Long roleIdFk, String grpPerm);
    void deleteByRoleIdFk(Long roleIdFk);
}
