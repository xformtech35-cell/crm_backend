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
                            String senderName = leadNode.path("SENDER_NAME").asText("").trim();
                            String senderCompany = leadNode.path("SENDER_COMPANY").asText("").trim();
                            if (senderCompany.isBlank() || senderCompany.equalsIgnoreCase("null")) {
                                senderCompany = leadNode.path("GLUSR_USR_COMPANYNAME").asText("").trim();
                            }
                            
                            String queryMessage = leadNode.path("QUERY_MESSAGE").asText("").trim();
                            if (queryMessage.isBlank()) {
                                queryMessage = leadNode.path("ENQ_MESSAGE").asText("").trim();
                            }
                            if (queryMessage.isBlank()) {
                                queryMessage = leadNode.path("SUBJECT").asText("").trim();
                            }
                            queryMessage = cleanText(queryMessage);

                            String productName = leadNode.path("QUERY_PRODUCT_NAME").asText("").trim();
                            if (productName.isBlank()) {
                                productName = leadNode.path("PRODUCT_NAME").asText("").trim();
                            }
                            if (!productName.isBlank() && !productName.equalsIgnoreCase("null") && !queryMessage.toLowerCase().contains(productName.toLowerCase())) {
                                queryMessage = "Product: " + productName + (queryMessage.isBlank() ? "" : "\n" + queryMessage);
                            }

                            String queryTimeStr = leadNode.path("QUERY_TIME").asText("").trim();
                            if (queryTimeStr.isBlank()) {
                                queryTimeStr = leadNode.path("DATE_TIME_RE").asText("").trim();
                            }
                            if (queryTimeStr.isBlank()) {
                                queryTimeStr = leadNode.path("QUERY_TIME_STAMP").asText("").trim();
                            }

                            java.time.LocalDate inquiryLocalDate = java.time.LocalDate.now();
                            LocalDateTime createdLocalDateTime = LocalDateTime.now();

                            if (!queryTimeStr.isBlank()) {
                                try {
                                    if (queryTimeStr.contains(" ")) {
                                        String datePart = queryTimeStr.split(" ")[0];
                                        if (datePart.contains("-") && datePart.length() == 10) {
                                            inquiryLocalDate = java.time.LocalDate.parse(datePart);
                                        }
                                    } else if (queryTimeStr.length() >= 10 && queryTimeStr.contains("-")) {
                                        inquiryLocalDate = java.time.LocalDate.parse(queryTimeStr.substring(0, 10));
                                    }
                                } catch (Exception ignored) {}
                            }

                            String contactPersonName = null;
                            if (!senderName.isBlank() && !senderName.equalsIgnoreCase("null")) {
                                if (!senderCompany.isBlank() && !senderCompany.equalsIgnoreCase("null") && !senderName.equalsIgnoreCase(senderCompany)) {
                                    contactPersonName = senderName;
                                } else if (senderCompany.isBlank() || senderCompany.equalsIgnoreCase("null")) {
                                    senderCompany = senderName;
                                    contactPersonName = senderName;
                                }
                            } else {
                                senderName = !senderCompany.isBlank() && !senderCompany.equalsIgnoreCase("null") ? senderCompany : "IndiaMART Buyer";
                                if (senderCompany.isBlank() || senderCompany.equalsIgnoreCase("null")) {
                                    senderCompany = senderName;
                                }
                            }

                            Lead lead = Lead.builder()
                                    .uniqueQueryId(queryId)
                                    .leadFirstName(senderName)
                                    .companyContactPersonName(contactPersonName != null ? contactPersonName : senderName)
                                    .leadMobileNo(leadNode.path("SENDER_MOBILE").asText(""))
                                    .leadEmail(leadNode.path("SENDER_EMAIL").asText(""))
                                    .leadCountry(leadNode.path("SENDER_COUNTRY_ISO").asText(""))
                                    .leadCity(leadNode.path("SENDER_CITY").asText(""))
                                    .leadState(leadNode.path("SENDER_STATE").asText(""))
                                    .leadOrganisationName(senderCompany)
                                    .enquiryDescription(queryMessage)
                                    .leadReason(queryMessage)
                                    .leadSource(com.crm.util.AppConstants.INDIAMART_SOURCE)
                                    .leadStatus(com.crm.util.AppConstants.INDIAMART_DEFAULT_STATUS)
                                    .leadOutcomeStatus("Open")
                                    .enquiryType("Product Enquiry")
                                    .enquiryStatus("Pending")
                                    .inquiryDate(inquiryLocalDate)
                                    .leadCreatedDate(createdLocalDateTime)
                                    .createdBy("IndiaMART Integration")
                                    .updatedBy("IndiaMART Integration")
                                    .userIdFk(config.getUserIdFk())
                                    .leadAssignedMember(config.getAutoAssignUserId())
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

    @jakarta.annotation.PostConstruct
    public void repairExistingIndiamartLeads() {
        try {
            List<Lead> allLeads = leadRepository.findAll();
            boolean changed = false;
            for (Lead lead : allLeads) {
                boolean leadModified = false;
                
                // Fix leadStatus if it's "New" or "None" or null
                if (lead.getLeadStatus() == null || lead.getLeadStatus().isBlank() || "New".equalsIgnoreCase(lead.getLeadStatus()) || "None".equalsIgnoreCase(lead.getLeadStatus())) {
                    lead.setLeadStatus("New Lead");
                    leadModified = true;
                }

                // Fix companyContactPersonName if null/blank
                if ((lead.getCompanyContactPersonName() == null || lead.getCompanyContactPersonName().isBlank() || "-".equals(lead.getCompanyContactPersonName()))
                        && lead.getLeadFirstName() != null && !lead.getLeadFirstName().isBlank()) {
                    lead.setCompanyContactPersonName(lead.getLeadFirstName());
                    leadModified = true;
                }

                // Fix leadOrganisationName if null/blank
                if ((lead.getLeadOrganisationName() == null || lead.getLeadOrganisationName().isBlank() || "-".equals(lead.getLeadOrganisationName()))
                        && lead.getCompanyContactPersonName() != null && !lead.getCompanyContactPersonName().isBlank()) {
                    lead.setLeadOrganisationName(lead.getCompanyContactPersonName());
                    leadModified = true;
                }

                // Fix enquiryDescription if null/blank or contains %28 / <br>
                String currentDesc = lead.getEnquiryDescription();
                if ((currentDesc == null || currentDesc.isBlank() || "-".equals(currentDesc))
                        && lead.getLeadReason() != null && !lead.getLeadReason().isBlank()) {
                    currentDesc = lead.getLeadReason();
                }
                if (currentDesc != null && !currentDesc.isBlank()) {
                    String cleaned = cleanText(currentDesc);
                    if (!cleaned.equals(currentDesc)) {
                        lead.setEnquiryDescription(cleaned);
                        lead.setLeadReason(cleaned);
                        leadModified = true;
                    }
                }

                // Fix leadSource if "India MART" or "Indiamart"
                if ("India MART".equalsIgnoreCase(lead.getLeadSource()) || "Indiamart".equalsIgnoreCase(lead.getLeadSource())) {
                    lead.setLeadSource("IndiaMART");
                    leadModified = true;
                }

                if (leadModified) {
                    leadRepository.save(lead);
                    changed = true;
                }
            }
            if (changed) {
                log.info("Successfully repaired existing IndiaMART leads in database.");
            }
        } catch (Exception e) {
            log.error("Error repairing existing IndiaMART leads: {}", e.getMessage());
        }
    }

    private String cleanText(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String text = raw.trim();
        try {
            text = java.net.URLDecoder.decode(text, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            // ignore
        }
        text = text.replaceAll("(?i)<br\\s*/?>", "\n");
        text = text.replaceAll("(?i)&nbsp;", " ");
        text = text.replaceAll("<[^>]*>", "");
        text = text.replaceAll("\n\\s*\n", "\n");
        return text.trim();
    }
}
