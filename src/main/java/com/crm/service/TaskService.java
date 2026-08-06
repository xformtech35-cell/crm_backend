package com.crm.service;

import com.crm.dto.request.TaskRequest;
import com.crm.entity.Task;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.TaskRepository;
import com.crm.util.AuthUtil;
import com.crm.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.crm.entity.User;
import com.crm.repository.UserRepository;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final FileUploadUtil fileUploadUtil;
    private final AuthUtil authUtil;
    private final UserRepository userRepository;
    @org.springframework.context.annotation.Lazy
    private final LeadService leadService;

    /**
     * Data isolation:
     * - SUPER_ADMIN: all tasks
     * - ADMIN: only tasks created under their company (userIdFk == userId)
     * - TEAM_LEAD: tasks assigned to/created by members of their team
     * - USER: only tasks assigned to them
     */
    public List<Task> getAllTasks(Long userId, String role) {
        if (authUtil.isSuperAdmin(role)) {
            return taskRepository.findAll();
        }
        User user = userRepository.findById(userId).orElse(null);
        String scopeMode = authUtil.resolveDataScopeMode(user, "TASKS");

        if ("ALL_DATA".equals(scopeMode)) {
            List<Long> companyUserIds = leadService.getCompanyUserIds(userId, role);
            return taskRepository.findByTaskAssignedMemberInOrTaskAssignedToInOrUserIdFkIn(companyUserIds, companyUserIds, companyUserIds);
        }
        if ("TEAM_DATA".equals(scopeMode)) {
            List<Long> teamUserIds = authUtil.getTeamLeadMemberUserIds(user);
            List<Long> teamIds = authUtil.getTeamLeadTeamIds(user);
            if (teamUserIds.isEmpty()) teamUserIds = List.of(-1L);
            if (teamIds.isEmpty()) teamIds = List.of(-1L);
            return taskRepository.findByTeamLeadCriteria(teamUserIds, teamIds);
        }
        // OWN_DATA_ONLY
        return taskRepository.findByTaskAssignedMemberOrTaskAssignedToOrUserIdFk(userId, userId, userId);
    }

    public Task getById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));
    }

    public void validateTaskScopeAccess(Task task, User user) {
        if (user == null || authUtil.isSuperAdmin(user.getRole())) return;
        String scopeMode = authUtil.resolveDataScopeMode(user, "TASKS");
        if ("ALL_DATA".equals(scopeMode)) return;

        if ("TEAM_DATA".equals(scopeMode)) {
            List<Long> teamUserIds = authUtil.getTeamLeadMemberUserIds(user);
            boolean isCreatorOrAssignedInTeam = (task.getUserIdFk() != null && teamUserIds.contains(task.getUserIdFk()))
                    || (task.getTaskAssignedMember() != null && teamUserIds.contains(task.getTaskAssignedMember()))
                    || (task.getTaskAssignedTo() != null && teamUserIds.contains(task.getTaskAssignedTo()));
            if (!isCreatorOrAssignedInTeam) {
                throw new org.springframework.security.access.AccessDeniedException("Access denied: You do not have team permission to view or modify this task.");
            }
        } else if ("OWN_DATA_ONLY".equals(scopeMode)) {
            boolean isOwner = (task.getUserIdFk() != null && task.getUserIdFk().equals(user.getUserid()))
                    || (task.getTaskAssignedMember() != null && task.getTaskAssignedMember().equals(user.getUserid()))
                    || (task.getTaskAssignedTo() != null && task.getTaskAssignedTo().equals(user.getUserid()));
            if (!isOwner) {
                throw new org.springframework.security.access.AccessDeniedException("Access denied: You are restricted to your own tasks only.");
            }
        }
    }

    public List<Task> getByTeam(Long teamId) {
        return taskRepository.findByTaskAssignedTeam(teamId);
    }

    public Task create(TaskRequest req, Long userId, MultipartFile doc) throws IOException {
        Task task = mapToEntity(req, new Task());
        task.setUserIdFk(userId);

        Long assignedId = task.getTaskAssignedMember() != null ? task.getTaskAssignedMember() : task.getTaskAssignedTo();
        if (assignedId == null) {
            assignedId = userId;
        }
        task.setTaskAssignedMember(assignedId);
        task.setTaskAssignedTo(assignedId);

        if (doc != null && !doc.isEmpty()) task.setTaskDoc(fileUploadUtil.upload(doc));
        return taskRepository.save(task);
    }

    public Task update(Long id, TaskRequest req, MultipartFile doc) throws IOException {
        Task task = getById(id);
        mapToEntity(req, task);

        Long assignedId = task.getTaskAssignedMember() != null ? task.getTaskAssignedMember() : task.getTaskAssignedTo();
        if (assignedId != null) {
            task.setTaskAssignedMember(assignedId);
            task.setTaskAssignedTo(assignedId);
        }

        if (doc != null && !doc.isEmpty()) task.setTaskDoc(fileUploadUtil.upload(doc));
        return taskRepository.save(task);
    }

    public void delete(Long id) {
        taskRepository.delete(getById(id));
    }

    private Task mapToEntity(TaskRequest req, Task task) {
        task.setTaskName(req.getTaskName());
        task.setTaskAssignedMember(req.getTaskAssignedMember());
        task.setTaskAssignedTeam(req.getTaskAssignedTeam());
        task.setTaskAssign(req.getTaskAssign());
        task.setTaskAssignedTo(req.getTaskAssignedTo());
        task.setTaskStartDate(req.getTaskStartDate());
        task.setTaskCompletedDate(req.getTaskCompletedDate());
        task.setTaskDueDate(req.getTaskDueDate());
        task.setTaskRelatedTo(req.getTaskRelatedTo());
        task.setTaskDescription(req.getTaskDescription());
        task.setTaskPriority(req.getTaskPriority());
        task.setTaskPercentageCompleted(req.getTaskPercentageCompleted());
        // New fields
        task.setTaskType(req.getTaskType());
        task.setTaskPhone(req.getTaskPhone());
        task.setTaskEmail(req.getTaskEmail());
        task.setTaskProjectId(req.getTaskProjectId());
        task.setTaskExpectedCompletion(req.getTaskExpectedCompletion());
        task.setTaskPeriod(req.getTaskPeriod());
        if (req.getTaskTimeSpentMinutes() != null) task.setTaskTimeSpentMinutes(req.getTaskTimeSpentMinutes());
        return task;
    }
}
