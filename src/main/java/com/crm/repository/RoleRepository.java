package com.crm.repository;

import com.crm.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findFirstByRoleName(String roleName);
    default Optional<Role> findByRoleName(String roleName) {
        return findFirstByRoleName(roleName);
    }
    boolean existsByRoleName(String roleName);
    List<Role> findByUserIdFk(Long userIdFk);
    List<Role> findByUserIdFkOrUserIdFkIsNull(Long userIdFk);
    boolean existsByRoleNameAndUserIdFk(String roleName, Long userIdFk);
}
