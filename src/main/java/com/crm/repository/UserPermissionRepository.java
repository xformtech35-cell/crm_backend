package com.crm.repository;

import com.crm.entity.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {
    List<UserPermission> findByUserIdFk(Long userIdFk);
    boolean existsByUserIdFk(Long userIdFk);
    void deleteByUserIdFk(Long userIdFk);
}
