package com.crm.service;

import com.crm.dto.request.TeamMemberRequest;
import com.crm.entity.TeamMember;
import com.crm.entity.User;
import com.crm.exception.BadRequestException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.RoleRepository;
import com.crm.repository.TeamMemberRepository;
import com.crm.repository.UserRepository;
import com.crm.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TeamMemberService {

    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthUtil authUtil;

    public List<TeamMember> getAllTeamMembers(Long userId, String role) {
        List<TeamMember> members;
        if (authUtil.isSuperAdmin(role)) {
            members = teamMemberRepository.findAll();
        } else {
            members = teamMemberRepository.findByUserIdFk(userId);
        }
        for (TeamMember member : members) {
            populateUserFields(member);
        }
        return members;
    }

    public TeamMember getById(Long id) {
        TeamMember member = teamMemberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TeamMember", "id", id));
        populateUserFields(member);
        return member;
    }

    public TeamMember getById(Long id, User currentUser) {
        TeamMember member = getById(id);
        validateTeamMemberAccess(member, currentUser);
        return member;
    }

    private boolean isRoleAdminOrSuperAdmin(Long roleId) {
        if (roleId == null) return false;
        return roleRepository.findById(roleId)
                .map(r -> "ADMIN".equalsIgnoreCase(r.getRoleName()) || "SUPER_ADMIN".equalsIgnoreCase(r.getRoleName()) || "SUPER ADMIN".equalsIgnoreCase(r.getRoleName()))
                .orElse(false);
    }

    @Transactional
    public TeamMember create(TeamMemberRequest req, User currentUser) {
        String currentRole = currentUser.getRole();
        if (!authUtil.isAnyAdmin(currentRole)) {
            throw new AccessDeniedException("Access denied");
        }

        if (authUtil.isAdmin(currentRole)) {
            if (isRoleAdminOrSuperAdmin(req.getTeamMemberRole())) {
                throw new AccessDeniedException("Admins cannot create Admin or Super Admin members");
            }
        }

        if (userRepository.existsByUserEmail(req.getTeamMemberEmail())) {
            throw new BadRequestException("Email is already registered: " + req.getTeamMemberEmail());
        }

        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new BadRequestException("Password is required for new team members");
        }

        // Also create a User account so the team member can login
        User user = User.builder()
                .username(req.getTeamMemberEmail())
                .userEmail(req.getTeamMemberEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(req.getTeamMemberRole() != null ? req.getTeamMemberRole().toString() : "member")
                .createdDate(LocalDate.now())
                .build();
        userRepository.save(user);

        TeamMember member = TeamMember.builder()
                .teamMemberName(req.getTeamMemberName())
                .teamMemberRole(req.getTeamMemberRole())
                .teamMemberMobile(req.getTeamMemberMobile())
                .teamMemberEmail(req.getTeamMemberEmail())
                .userIdFk(currentUser.getUserid())
                .build();
        TeamMember saved = teamMemberRepository.save(member);
        populateUserFields(saved);
        return saved;
    }

    @Transactional
    public TeamMember update(Long id, TeamMemberRequest req, User currentUser) {
        String currentRole = currentUser.getRole();
        if (!authUtil.isAnyAdmin(currentRole)) {
            throw new AccessDeniedException("Access denied");
        }

        TeamMember member = getById(id);
        validateTeamMemberAccess(member, currentUser);
        
        User targetUser = userRepository.findByUserEmail(member.getTeamMemberEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", member.getTeamMemberEmail()));

        if (authUtil.isAdmin(currentRole)) {
            if (authUtil.isAnyAdmin(targetUser.getRole())) {
                throw new AccessDeniedException("Admins cannot edit Admin or Super Admin members");
            }
            if (isRoleAdminOrSuperAdmin(req.getTeamMemberRole())) {
                throw new AccessDeniedException("Admins cannot assign Admin or Super Admin roles");
            }
        }

        if (!member.getTeamMemberEmail().equalsIgnoreCase(req.getTeamMemberEmail())) {
            if (userRepository.existsByUserEmail(req.getTeamMemberEmail())) {
                throw new BadRequestException("Email is already registered: " + req.getTeamMemberEmail());
            }
            targetUser.setUserEmail(req.getTeamMemberEmail());
            targetUser.setUsername(req.getTeamMemberEmail());
        }
        if (req.getTeamMemberRole() != null) {
            targetUser.setRole(req.getTeamMemberRole().toString());
        }
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            targetUser.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        userRepository.save(targetUser);

        member.setTeamMemberName(req.getTeamMemberName());
        member.setTeamMemberRole(req.getTeamMemberRole());
        member.setTeamMemberMobile(req.getTeamMemberMobile());
        member.setTeamMemberEmail(req.getTeamMemberEmail());
        TeamMember saved = teamMemberRepository.save(member);
        populateUserFields(saved);
        return saved;
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        String currentRole = currentUser.getRole();
        if (!authUtil.isAnyAdmin(currentRole)) {
            throw new AccessDeniedException("Access denied");
        }

        TeamMember member = getById(id);
        validateTeamMemberAccess(member, currentUser);
        
        User targetUser = userRepository.findByUserEmail(member.getTeamMemberEmail()).orElse(null);

        if (authUtil.isAdmin(currentRole)) {
            if (targetUser != null && authUtil.isAnyAdmin(targetUser.getRole())) {
                throw new AccessDeniedException("Admins cannot delete Admin or Super Admin members");
            }
        }

        if (targetUser != null) {
            userRepository.delete(targetUser);
        }
        teamMemberRepository.delete(member);
    }

    private void validateTeamMemberAccess(TeamMember member, User currentUser) {
        if (!authUtil.isSuperAdmin(currentUser.getRole())) {
            if (member.getUserIdFk() == null || !member.getUserIdFk().equals(currentUser.getUserid())) {
                throw new AccessDeniedException("Access denied to this team member");
            }
        }
    }

    private void populateUserFields(TeamMember member) {
        if (member != null && member.getTeamMemberEmail() != null) {
            userRepository.findByUserEmail(member.getTeamMemberEmail()).ifPresent(user -> {
                member.setUserid(user.getUserid());
                member.setUsername(user.getUsername());
                member.setUserEmail(user.getUserEmail());
            });
        }
    }
}
