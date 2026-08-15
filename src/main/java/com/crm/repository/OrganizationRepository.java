package com.crm.repository;

import com.crm.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;


@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long>, JpaSpecificationExecutor<Organization> {
    java.util.List<Organization> findByUserIdFk(Long userIdFk);
    java.util.List<Organization> findByUserIdFkIn(java.util.List<Long> userIds);
    java.util.List<Organization> findByOrganizationNameAndUserIdFk(String organizationName, Long userIdFk);
}
