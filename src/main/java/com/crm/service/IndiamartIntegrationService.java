package com.crm.service;

import com.crm.entity.IntegrationConfig;
import com.crm.entity.Lead;
import com.crm.repository.IntegrationConfigRepository;
import com.crm.repository.LeadRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndiamartIntegrationService {

    private final IntegrationConfigRepository configRepository;
    private final LeadRepository leadRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Runs every 5 minutes 15 seconds (315000 ms)
    @Scheduled(fixedRate = 315000)
    public void fetchIndiamartLeads() {
        List<IntegrationConfig> activeConfigs = configRepository.findByNameAndEnabledTrue("INDIAMART");
        for (IntegrationConfig config : activeConfigs) {
            try {
                // Avoid calling the IndiaMART API if it was hit recently (within 5 minutes)
                // Use 290 seconds (4m 50s) to allow a small buffer for scheduling drift
                if (config.getLastSyncTime() != null 
                        && config.getLastSyncTime().isAfter(LocalDateTime.now().minusSeconds(290))) {
                    log.info("Skipping scheduled IndiaMART sync for config {} - last sync was at {}, which is within the 5-minute limit.", 
                            config.getId(), config.getLastSyncTime());
                    continue;
                }
                syncLeads(config);
            } catch (Exception e) {
                log.error("Scheduled IndiaMART sync failed for config {}: {}", config.getId(), e.getMessage());
            }
        }
    }

    public void syncLeads(IntegrationConfig config) {
        try {
            if (config.getApiKey() == null || config.getApiKey().isEmpty()) return;

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime start = config.getLastSyncTime() != null 
                    ? config.getLastSyncTime().minusMinutes(5) // overlap by 5 mins to catch edge cases
                    : now.minusDays(7).plusMinutes(5); // Default to last 7 days (maximum single request limit for IndiaMART)

            // Format: DD-MMM-YYYYHH:mm:ss (e.g. 01-Jan-202216:30:00)
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MMM-yyyyHH:mm:ss");
            
            String url = String.format("%s?glusr_crm_key=%s&start_time=%s&end_time=%s",
                    config.getApiUrl() != null && !config.getApiUrl().isEmpty() ? config.getApiUrl() : "https://mapi.indiamart.com/wservce/crm/crmListing/v2/",
                    config.getApiKey(),
                    start.format(fmt),
                    now.format(fmt)
            );

            log.info("Fetching IndiaMART leads for Admin ID {}. Range: {} to {}", config.getUserIdFk(), start.format(fmt), now.format(fmt));
            
            String jsonResponse = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(jsonResponse);

            if (root.has("CODE") && root.get("CODE").asInt() == 200) {
                JsonNode responses = root.get("RESPONSE");
                int newLeadsCount = 0;
                
                if (responses != null && responses.isArray()) {
                    for (JsonNode leadNode : responses) {
                        String queryId = leadNode.path("UNIQUE_QUERY_ID").asText();
                        
                        // Check if lead already exists to prevent duplicate insertion
                        if (!leadRepository.existsByUniqueQueryId(queryId)) {
                            Lead lead = Lead.builder()
                                    .uniqueQueryId(queryId)
                                    .leadFirstName(leadNode.path("SENDER_NAME").asText("IndiaMART Buyer"))
                                    .leadMobileNo(leadNode.path("SENDER_MOBILE").asText())
                                    .leadEmail(leadNode.path("SENDER_EMAIL").asText())
                                    .leadCountry(leadNode.path("SENDER_COUNTRY_ISO").asText())
                                    .leadCity(leadNode.path("SENDER_CITY").asText())
                                    .leadState(leadNode.path("SENDER_STATE").asText())
                                    .leadOrganisationName(leadNode.path("SENDER_COMPANY").asText())
                                    .leadReason(leadNode.path("QUERY_MESSAGE").asText())
                                    .leadSource("India MART")
                                    .leadStatus("New")
                                    .leadCreatedDate(LocalDateTime.now())
                                    .userIdFk(config.getUserIdFk())
                                    .leadAssignedMember(config.getAutoAssignUserId()) // Auto assignment!
                                    .build();

                            leadRepository.save(lead);
                            newLeadsCount++;
                        }
                    }
                }

                config.setLastSyncTime(now);
                config.setSyncStatus("SUCCESS");
                config.setLeadsPulled((config.getLeadsPulled() == null ? 0 : config.getLeadsPulled()) + newLeadsCount);
            } else if (root.has("CODE") && root.get("CODE").asInt() == 204) {
                // No new leads, but success
                config.setLastSyncTime(now);
                config.setSyncStatus("SUCCESS");
            } else {
                String errorMsg = root.has("MESSAGE") ? root.get("MESSAGE").asText() : "Unknown IndiaMART API response";
                log.error("IndiaMART API Error: {}", errorMsg);
                config.setSyncStatus("FAILURE");
                throw new RuntimeException("IndiaMART API Error: " + errorMsg);
            }

            configRepository.save(config);

        } catch (Exception e) {
            log.error("Error running IndiaMART integration for config {}: {}", config.getId(), e.getMessage());
            config.setSyncStatus("FAILURE");
            config.setLastSyncTime(LocalDateTime.now());
            configRepository.save(config);
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
