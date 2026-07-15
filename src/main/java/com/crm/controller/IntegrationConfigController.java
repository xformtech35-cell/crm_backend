package com.crm.controller;

import com.crm.dto.response.ApiResponse;
import com.crm.entity.IntegrationConfig;
import com.crm.entity.User;
import com.crm.repository.IntegrationConfigRepository;
import com.crm.service.IndiamartIntegrationService;
import com.crm.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/integrations")
@RequiredArgsConstructor
public class IntegrationConfigController {

    private final IntegrationConfigRepository repository;
    private final AuthUtil authUtil;
    private final IndiamartIntegrationService indiamartService;

    private void validateIntegrationsAccess(User user) {
        if (authUtil.isSuperAdmin(user.getRole())) {
            return;
        }
        if (!authUtil.isAdmin(user.getRole())) {
            throw new AccessDeniedException("Access Denied: Only company admins can manage integrations");
        }
        if (!user.isIntegrationsAccess()) {
            throw new AccessDeniedException("Access Denied: Integrations are not enabled for your company");
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<IntegrationConfig>>> getAllConfigs(Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        validateIntegrationsAccess(user);
        
        Long adminId = user.getUserid(); 
        
        List<IntegrationConfig> configs = repository.findByUserIdFk(adminId);
        if (configs.isEmpty()) {
            // Seed default templates if empty
            IntegrationConfig indiamart = IntegrationConfig.builder()
                    .name("INDIAMART")
                    .userIdFk(adminId)
                    .enabled(false)
                    .apiKey("hsdvhjasbhdbJHHBHBHBJBJHBHBHB=")
                    .apiUrl("https://mapi.indiamart.com/wservce/crm/crmListing/v2/")
                    .additionalConfig("{\"mobileNumber\":\"\",\"syncFrequency\":\"1 Hour\"}")
                    .leadsPulled(0)
                    .build();

            IntegrationConfig tradeindia = IntegrationConfig.builder()
                    .name("TRADEINDIA")
                    .userIdFk(adminId)
                    .enabled(false)
                    .apiKey("")
                    .apiUrl("https://www.tradeindia.com/utils/my_inquiry.html")
                    .additionalConfig("{\"syncFrequency\":\"1 Hour\"}")
                    .leadsPulled(0)
                    .build();

            IntegrationConfig whatsapp = IntegrationConfig.builder()
                    .name("WHATSAPP")
                    .userIdFk(adminId)
                    .enabled(false)
                    .apiKey("")
                    .apiUrl("https://graph.facebook.com/v19.0/")
                    .additionalConfig("{\"phoneNumberId\":\"\",\"senderNumber\":\"\"}")
                    .leadsPulled(0)
                    .build();

            repository.save(indiamart);
            repository.save(tradeindia);
            repository.save(whatsapp);
            configs = repository.findByUserIdFk(adminId);
        }
        return ResponseEntity.ok(ApiResponse.success("Integrations fetched successfully", configs));
    }

    @PutMapping("/{name}")
    public ResponseEntity<ApiResponse<IntegrationConfig>> updateConfig(
            @PathVariable String name,
            @RequestBody IntegrationConfig updated,
            Authentication auth) {
            
        User user = authUtil.getCurrentUser(auth);
        validateIntegrationsAccess(user);
        Long adminId = user.getUserid();
        
        Optional<IntegrationConfig> existingOpt = repository.findByNameAndUserIdFk(name.toUpperCase(), adminId);
        IntegrationConfig config;
        
        if (existingOpt.isPresent()) {
            config = existingOpt.get();
            config.setEnabled(updated.isEnabled());
            // Reset last sync time if key is changed to fetch historical leads (up to 7 days)
            if (updated.getApiKey() != null && !updated.getApiKey().equals(config.getApiKey())) {
                config.setLastSyncTime(null);
                config.setSyncStatus(null);
            }
            config.setApiKey(updated.getApiKey());
            config.setApiUrl(updated.getApiUrl());
            config.setAdditionalConfig(updated.getAdditionalConfig());
            config.setAutoAssignUserId(updated.getAutoAssignUserId());
        } else {
            config = updated;
            config.setName(name.toUpperCase());
            config.setUserIdFk(adminId);
            config.setLeadsPulled(0);
        }
        
        repository.save(config);
        return ResponseEntity.ok(ApiResponse.success("Integration updated successfully", config));
    }

    @PostMapping("/{name}/test")
    public ResponseEntity<ApiResponse<String>> testConnection(
            @PathVariable String name,
            Authentication auth) {
        
        User user = authUtil.getCurrentUser(auth);
        validateIntegrationsAccess(user);
        
        Optional<IntegrationConfig> existingOpt = repository.findByNameAndUserIdFk(name.toUpperCase(), user.getUserid());
        
        if (existingOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Integration not configured"));
        }
        
        IntegrationConfig config = existingOpt.get();
        if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("API Key is missing"));
        }
        
        if ("INDIAMART".equals(name.toUpperCase())) {
            // Check rate limits to avoid hitting IndiaMART's 5-minute frequency block
            if (config.getLastSyncTime() != null 
                    && config.getLastSyncTime().isAfter(java.time.LocalDateTime.now().minusMinutes(5))) {
                
                if ("SUCCESS".equals(config.getSyncStatus())) {
                    return ResponseEntity.ok(ApiResponse.success("Connection test passed (cached)", "SUCCESS"));
                } else {
                    long secondsElapsed = java.time.Duration.between(config.getLastSyncTime(), java.time.LocalDateTime.now()).getSeconds();
                    long secondsRemaining = 300 - secondsElapsed;
                    if (secondsRemaining > 0) {
                        return ResponseEntity.badRequest().body(ApiResponse.error(
                            "Please wait " + (secondsRemaining / 60 > 0 ? (secondsRemaining / 60) + "m " : "") + (secondsRemaining % 60) + "s before requesting IndiaMART API again. (Rate limit: 1 request every 5 minutes)"
                        ));
                    }
                }
            }
            
            try {
                org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                java.time.LocalDateTime start = now.minusMinutes(15);
                java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyyHH:mm:ss");
                
                String url = String.format("%s?glusr_crm_key=%s&start_time=%s&end_time=%s",
                        config.getApiUrl() != null && !config.getApiUrl().isEmpty() ? config.getApiUrl() : "https://mapi.indiamart.com/wservce/crm/crmListing/v2/",
                        config.getApiKey(),
                        start.format(fmt),
                        now.format(fmt)
                );
                
                String jsonResponse = restTemplate.getForObject(url, String.class);
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(jsonResponse);
                
                // Update sync metadata
                config.setLastSyncTime(now);
                
                if (root.has("CODE")) {
                    int code = root.get("CODE").asInt();
                    if (code == 200 || code == 204) {
                        config.setSyncStatus("SUCCESS");
                        repository.save(config);
                        return ResponseEntity.ok(ApiResponse.success("Connection test passed", "SUCCESS"));
                    } else {
                        String msg = root.has("MESSAGE") ? root.get("MESSAGE").asText() : "API key validation failed";
                        config.setSyncStatus("FAILURE");
                        repository.save(config);
                        return ResponseEntity.badRequest().body(ApiResponse.error("IndiaMART API Error: " + msg));
                    }
                } else {
                    config.setSyncStatus("FAILURE");
                    repository.save(config);
                    return ResponseEntity.badRequest().body(ApiResponse.error("Invalid response from IndiaMART API"));
                }
            } catch (Exception e) {
                config.setLastSyncTime(java.time.LocalDateTime.now());
                config.setSyncStatus("FAILURE");
                repository.save(config);
                return ResponseEntity.badRequest().body(ApiResponse.error("Connection failed: " + e.getMessage()));
            }
        }
        
        return ResponseEntity.ok(ApiResponse.success("Connection test passed (mocked)", "SUCCESS"));
    }

    @PostMapping("/{name}/sync")
    public ResponseEntity<ApiResponse<String>> triggerSync(
            @PathVariable String name,
            Authentication auth) {
        
        User user = authUtil.getCurrentUser(auth);
        validateIntegrationsAccess(user);
        
        Optional<IntegrationConfig> existingOpt = repository.findByNameAndUserIdFk(name.toUpperCase(), user.getUserid());
        
        if (existingOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Integration not configured"));
        }
        
        IntegrationConfig config = existingOpt.get();
        if (!config.isEnabled()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Integration is disabled. Please enable it first."));
        }
        
        if ("INDIAMART".equals(name.toUpperCase())) {
            // Check rate limit to avoid IndiaMART 5-minute block
            if (config.getLastSyncTime() != null 
                    && config.getLastSyncTime().isAfter(java.time.LocalDateTime.now().minusMinutes(5))) {
                long secondsElapsed = java.time.Duration.between(config.getLastSyncTime(), java.time.LocalDateTime.now()).getSeconds();
                long secondsRemaining = 300 - secondsElapsed;
                if (secondsRemaining > 0) {
                    return ResponseEntity.badRequest().body(ApiResponse.error(
                        "Please wait " + (secondsRemaining / 60 > 0 ? (secondsRemaining / 60) + "m " : "") + (secondsRemaining % 60) + "s before syncing again. (Rate limit: 1 sync every 5 minutes)"
                    ));
                }
            }
            
            try {
                indiamartService.syncLeads(config);
                return ResponseEntity.ok(ApiResponse.success("Sync completed successfully", "SUCCESS"));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Sync failed: " + e.getMessage()));
            }
        }
        
        return ResponseEntity.ok(ApiResponse.success("Sync completed (mocked)", "SUCCESS"));
    }
}
