package com.crm.controller;

import com.crm.dto.response.ApiResponse;
import com.crm.entity.User;
import com.crm.exception.BadRequestException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.crm.service.RoleService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/superadmin/companies")
@RequiredArgsConstructor
public class SuperAdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> getAllCompanies() {
        List<User> companies = userRepository.findByRole("ADMIN");
        return ResponseEntity.ok(ApiResponse.success("Companies fetched successfully", companies));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<User>> createCompany(@RequestBody User user) {
        if (userRepository.existsByUserEmail(user.getUserEmail())) {
            throw new BadRequestException("Email already in use");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new BadRequestException("Password is required");
        }

        user.setRole("ADMIN");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedDate(LocalDate.now());

        if (user.getSubscriptionStatus() == null) {
            user.setSubscriptionStatus("Active");
        }

        User savedUser = userRepository.save(user);
        roleService.copyTemplateRolesToCompany(savedUser);
        // Sync integrations permissions on ADMIN role based on integrationsAccess flag
        roleService.syncIntegrationsPermissionFromCompanyAccess();
        return ResponseEntity.ok(ApiResponse.success("Company created successfully", savedUser));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> updateCompany(@PathVariable Long id, @RequestBody User updated) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", id));

        if (!existing.getUserEmail().equalsIgnoreCase(updated.getUserEmail()) &&
                userRepository.existsByUserEmail(updated.getUserEmail())) {
            throw new BadRequestException("Email already in use");
        }

        existing.setUsername(updated.getUsername());
        existing.setUserEmail(updated.getUserEmail());
        existing.setPlanName(updated.getPlanName());
        existing.setPlanPrice(updated.getPlanPrice());
        existing.setPlanValidity(updated.getPlanValidity());
        existing.setSubscriptionStatus(updated.getSubscriptionStatus());
        existing.setIntegrationsAccess(updated.isIntegrationsAccess());

        if (updated.getPassword() != null && !updated.getPassword().trim().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(updated.getPassword()));
        }

        User savedUser = userRepository.save(existing);
        // Bidirectional sync: keep global ADMIN role integrations permissions in sync with company flags
        roleService.syncIntegrationsPermissionFromCompanyAccess();
        return ResponseEntity.ok(ApiResponse.success("Company updated successfully", savedUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCompany(@PathVariable Long id) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", id));
        
        userRepository.delete(existing);
        return ResponseEntity.ok(ApiResponse.success("Company deleted successfully", null));
    }
}
