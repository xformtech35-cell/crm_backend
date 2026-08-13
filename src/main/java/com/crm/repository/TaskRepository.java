package com.crm.repository;

import com.crm.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    List<Task> findByUserIdFk(Long userIdFk);
    List<Task> findByTaskAssignedTeam(Long taskAssignedTeam);
    List<Task> findByTaskDueDate(String dueDate);
    List<Task> findByTaskDueDateStartingWith(String dueDatePrefix);
    List<Task> findByTaskAssignedMemberOrTaskAssignedTo(Long memberId, Long assignedTo);
    List<Task> findByTaskAssignedMemberOrTaskAssignedToOrUserIdFk(Long memberId, Long assignedTo, Long userIdFk);
    List<Task> findByTaskAssignedMemberInOrTaskAssignedToInOrUserIdFkIn(List<Long> memberIds, List<Long> assignedTos, List<Long> userIds);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT t FROM Task t WHERE (t.userIdFk IN :userIds OR t.taskAssignedMember IN :userIds OR t.taskAssignedTo IN :userIds OR t.taskAssignedTeam IN :teamIds) ORDER BY t.taskId DESC")
    List<Task> findByTeamLeadCriteria(@org.springframework.data.repository.query.Param("userIds") List<Long> userIds, @org.springframework.data.repository.query.Param("teamIds") List<Long> teamIds);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT t FROM Task t WHERE " +
           "(t.userIdFk = :userId OR " +
           "t.taskAssignedMember = :userId OR " +
           "t.taskAssignedTo = :userId OR " +
           "(:teamMemberId IS NOT NULL AND (t.taskAssignedMember = :teamMemberId OR t.taskAssignedTo = :teamMemberId)) OR " +
           "(:userEmail IS NOT NULL AND LOWER(t.taskCreatedBy) = LOWER(:userEmail))) " +
           "ORDER BY t.taskId DESC")
    List<Task> findByOwnDataCriteria(
        @org.springframework.data.repository.query.Param("userId") Long userId,
        @org.springframework.data.repository.query.Param("teamMemberId") Long teamMemberId,
        @org.springframework.data.repository.query.Param("userEmail") String userEmail
    );
    List<Task> findByTaskRelatedTo(String taskRelatedTo);
    List<Task> findByUserIdFkAndTaskDueDate(Long userIdFk, String dueDate);
    List<Task> findByUserIdFkAndTaskDueDateStartingWith(Long userIdFk, String dueDatePrefix);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Task t WHERE (t.emailSent IS NULL OR t.emailSent = false) AND t.taskDueDate IS NOT NULL")
    List<Task> findPendingTaskReminders();
}
