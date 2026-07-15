package com.crm.repository;

import com.crm.entity.TaskTimeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskTimeLogRepository extends JpaRepository<TaskTimeLog, Long> {
    List<TaskTimeLog> findByUserIdFk(Long userIdFk);
    List<TaskTimeLog> findByTaskId(Long taskId);
    List<TaskTimeLog> findByUserId(Long userId);
}
