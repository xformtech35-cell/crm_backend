package com.crm.service;

import com.crm.dto.request.ProjectRequest;
import com.crm.entity.Project;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.ProjectRepository;
import com.crm.util.FileUploadUtil;
import com.crm.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final FileUploadUtil fileUploadUtil;
    private final AuthUtil authUtil;
    private final com.crm.repository.UserRepository userRepository;
    private final LeadService leadService;

    public List<Project> getAllProjects(Long userId, String role) {
        List<Project> projects;
        if (authUtil.isSuperAdmin(role)) {
            projects = projectRepository.findAll();
        } else {
            com.crm.entity.User user = userRepository.findById(userId).orElse(null);
            String scopeMode = authUtil.resolveDataScopeMode(user, "PROJECTS");

            if ("ALL_DATA".equals(scopeMode)) {
                List<Long> companyUserIds = leadService.getCompanyUserIds(userId, role);
                projects = projectRepository.findByUserIdFkIn(companyUserIds);
            } else if ("TEAM_DATA".equals(scopeMode)) {
                List<Long> teamUserIds = authUtil.getTeamLeadMemberUserIds(user);
                if (teamUserIds.isEmpty()) teamUserIds = List.of(-1L);
                projects = projectRepository.findByUserIdFkIn(teamUserIds);
            } else {
                projects = projectRepository.findByUserIdFk(userId);
            }
        }

        // Fix 7: Auto-create monthly default project if zero projects exist
        if (projects.isEmpty() && userId != null) {
            Project defaultProj = createMonthlyDefaultProject(userId);
            projects = List.of(defaultProj);
        }

        return projects;
    }

    private Project createMonthlyDefaultProject(Long adminUserId) {
        LocalDate now = LocalDate.now();
        String monthYearStr = now.format(DateTimeFormatter.ofPattern("MMM yyyy"));
        String defaultName = "General Sales — " + monthYearStr;

        Project defaultProject = Project.builder()
                .projectName(defaultName)
                .projectCode("PROJ-" + now.getYear() + "-" + String.format("%02d", now.getMonthValue()))
                .projectStatus("In Progress")
                .projectStartDate(now.withDayOfMonth(1))
                .forecastCompletedDate(now.with(TemporalAdjusters.lastDayOfMonth()))
                .projectDescription("Default auto-created sales project for " + monthYearStr)
                .userIdFk(adminUserId)
                .build();

        return projectRepository.save(defaultProject);
    }

    public Project getById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
    }

    public Project create(ProjectRequest req, Long companyAdminId, MultipartFile doc) throws IOException {
        Project project = mapToEntity(req, new Project());
        project.setUserIdFk(companyAdminId);
        if (doc != null && !doc.isEmpty()) project.setProjectDoc(fileUploadUtil.upload(doc));
        return projectRepository.save(project);
    }

    public Project update(Long id, ProjectRequest req, MultipartFile doc) throws IOException {
        Project project = getById(id);
        mapToEntity(req, project);
        if (doc != null && !doc.isEmpty()) project.setProjectDoc(fileUploadUtil.upload(doc));
        return projectRepository.save(project);
    }

    public void delete(Long id) {
        projectRepository.delete(getById(id));
    }

    private Project mapToEntity(ProjectRequest req, Project project) {
        project.setProjectName(req.getProjectName());
        project.setProjectCode(req.getProjectCode());
        project.setOrganisationName(req.getOrganisationName());
        project.setProjectStatus(req.getProjectStatus());
        project.setProjectStartDate(req.getProjectStartDate());
        project.setProjectCompletedDate(req.getProjectCompletedDate());
        project.setForecastCompletedDate(req.getForecastCompletedDate());
        project.setProjectDescription(req.getProjectDescription());
        project.setOppIdFk(req.getOppIdFk());
        return project;
    }
}

