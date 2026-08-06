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
        if (authUtil.isSuperAdmin(role)) return projectRepository.findAll();
        com.crm.entity.User user = userRepository.findById(userId).orElse(null);
        String scopeMode = authUtil.resolveDataScopeMode(user, "PROJECTS");

        if ("ALL_DATA".equals(scopeMode)) {
            List<Long> companyUserIds = leadService.getCompanyUserIds(userId, role);
            return projectRepository.findByUserIdFkIn(companyUserIds);
        }
        if ("TEAM_DATA".equals(scopeMode)) {
            List<Long> teamUserIds = authUtil.getTeamLeadMemberUserIds(user);
            if (teamUserIds.isEmpty()) teamUserIds = List.of(-1L);
            return projectRepository.findByUserIdFkIn(teamUserIds);
        }
        return projectRepository.findByUserIdFk(userId);
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
