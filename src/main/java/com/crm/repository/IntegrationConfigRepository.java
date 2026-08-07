package com.crm.repository;

import com.crm.entity.IntegrationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface IntegrationConfigRepository extends JpaRepository<IntegrationConfig, Long> {
    List<IntegrationConfig> findByUserIdFk(Long userIdFk);
    Optional<IntegrationConfig> findFirstByNameAndUserIdFk(String name, Long userIdFk);
    default Optional<IntegrationConfig> findByNameAndUserIdFk(String name, Long userIdFk) {
        return findFirstByNameAndUserIdFk(name, userIdFk);
    }
    List<IntegrationConfig> findByNameAndEnabledTrue(String name);
}
