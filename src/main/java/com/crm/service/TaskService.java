package com.crm.service;

import com.crm.dto.request.BulkTaskUpdateRequest;
import com.crm.dto.request.TaskRequest;
import com.crm.entity.Task;
import com.crm.entity.User;
import com.crm.exception.BadRequestException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.TaskRepository;
import com.crm.repository.TeamMemberRepository;
import com.crm.repository.UserRepository;
import com.crm.util.AuthUtil;
import com.crm.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
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
    private final TeamMemberRepository teamMemberRepository;
    private final EmailService emailService;

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
        com.crm.entity.TeamMember tm = teamMemberRepository.findByTeamMemberEmail(user != null ? user.getUserEmail() : "").orElse(null);
        Long teamMemberId = tm != null ? tm.getTeamMemberId() : null;
        String userEmail = user != null ? user.getUserEmail() : null;
        return taskRepository.findByOwnDataCriteria(userId, teamMemberId, userEmail);
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

    private void validateTaskRequest(TaskRequest req) {
        if (req == null) {
            throw new BadRequestException("Task request payload is required.");
        }
        if (req.getTaskName() == null || req.getTaskName().trim().isEmpty()) {
            throw new BadRequestException("Task Name is mandatory.");
        }
        if (req.getTaskAssignedMember() == null) {
            throw new BadRequestException("Task Assignee is mandatory.");
        }
        if (req.getTaskDueDate() == null || req.getTaskDueDate().trim().isEmpty()) {
            throw new BadRequestException("Task Due Date is mandatory.");
        }
        if (req.getTaskType() == null || req.getTaskType().trim().isEmpty()) {
            throw new BadRequestException("Task Type is mandatory.");
        }
        if (req.getTaskRelatedTo() == null || req.getTaskRelatedTo().trim().isEmpty()) {
            throw new BadRequestException("Related Lead or Project reference is mandatory.");
        }
    }

    public Task create(TaskRequest req, Long userId, MultipartFile doc) throws IOException {
        validateTaskRequest(req);
        Task task = mapToEntity(req, new Task());
        task.setUserIdFk(userId);

        Long assignedId = task.getTaskAssignedMember() != null ? task.getTaskAssignedMember() : task.getTaskAssignedTo();
        if (assignedId == null) {
            assignedId = userId;
        }
        task.setTaskAssignedMember(assignedId);
        task.setTaskAssignedTo(assignedId);

        if (doc != null && !doc.isEmpty()) task.setTaskDoc(fileUploadUtil.upload(doc));
        Task saved = taskRepository.save(task);

        // Fix 2: Email notification on assignment
        User creator = userRepository.findById(userId).orElse(null);
        String assignerName = creator != null ? creator.getUsername() : "Admin";
        emailService.sendTaskAssignmentEmail(saved, saved.getTaskAssignedMember(), assignerName);

        return saved;
    }

    public Task update(Long id, TaskRequest req, MultipartFile doc) throws IOException {
        validateTaskRequest(req);
        Task task = getById(id);
        Long oldAssignee = task.getTaskAssignedMember();

        mapToEntity(req, task);

        Long assignedId = task.getTaskAssignedMember() != null ? task.getTaskAssignedMember() : task.getTaskAssignedTo();
        if (assignedId != null) {
            task.setTaskAssignedMember(assignedId);
            task.setTaskAssignedTo(assignedId);
        }

        if (doc != null && !doc.isEmpty()) task.setTaskDoc(fileUploadUtil.upload(doc));
        Task updated = taskRepository.save(task);

        // Fix 2: Email notification if assigned member changed
        if (assignedId != null && !assignedId.equals(oldAssignee)) {
            emailService.sendTaskAssignmentEmail(updated, assignedId, "Admin");
        }

        return updated;
    }

    @Transactional
    public List<Task> bulkUpdate(BulkTaskUpdateRequest req, User currentUser) {
        if (req == null || req.getTaskIds() == null || req.getTaskIds().isEmpty()) {
            throw new BadRequestException("Please select at least one task for bulk update.");
        }

        List<Task> updatedTasks = new ArrayList<>();
        for (Long id : req.getTaskIds()) {
            Task task = getById(id);
            // Fix 4 & Security Safeguard: Enforce role-based data scope isolation for bulk update
            validateTaskScopeAccess(task, currentUser);

            boolean assignedChanged = false;
            if (req.getTaskAssignedMember() != null) {
                task.setTaskAssignedMember(req.getTaskAssignedMember());
                task.setTaskAssignedTo(req.getTaskAssignedMember());
                assignedChanged = true;
            }
            if (req.getTaskAssignedTeam() != null) {
                task.setTaskAssignedTeam(req.getTaskAssignedTeam());
            }
            if (req.getTaskDueDate() != null && !req.getTaskDueDate().trim().isEmpty()) {
                task.setTaskDueDate(req.getTaskDueDate().trim());
            }
            if (req.getTaskPriority() != null && !req.getTaskPriority().trim().isEmpty()) {
                task.setTaskPriority(req.getTaskPriority().trim());
            }
            if (req.getTaskAssign() != null && !req.getTaskAssign().trim().isEmpty()) {
                task.setTaskAssign(req.getTaskAssign().trim());
            }

            Task saved = taskRepository.save(task);
            updatedTasks.add(saved);

            if (assignedChanged) {
                String assignerName = currentUser != null ? currentUser.getUsername() : "Admin";
                emailService.sendTaskAssignmentEmail(saved, saved.getTaskAssignedMember(), assignerName);
            }
        }
        return updatedTasks;
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

