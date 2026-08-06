package com.crm.repository;

import com.crm.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {
    List<Project> findByUserIdFk(Long userIdFk);
    List<Project> findByUserIdFkIn(List<Long> userIds);
    List<Project> findByProjectStatus(String projectStatus);
    long countByProjectStatus(String projectStatus);
    long countByUserIdFk(Long userIdFk);

    @Query("SELECT p.projectStatus AS status, COUNT(p) AS count FROM Project p GROUP BY p.projectStatus")
    List<Object[]> countGroupByStatus();

    @Query("SELECT p.projectStatus AS status, COUNT(p) AS count FROM Project p WHERE p.userIdFk = :userId GROUP BY p.projectStatus")
    List<Object[]> countGroupByStatusForUser(@Param("userId") Long userId);
}
