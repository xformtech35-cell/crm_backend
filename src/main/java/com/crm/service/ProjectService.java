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

    public List<Project> getAllProjects(Long companyAdminId, String role) {
        if (authUtil.isSuperAdmin(role)) return projectRepository.findAll();
        return projectRepository.findByUserIdFk(companyAdminId);
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
