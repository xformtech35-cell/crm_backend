package com.crm.service;

import com.crm.entity.IntegrationConfig;
import com.crm.repository.IntegrationConfigRepository;
import com.crm.repository.LeadRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeIndiaIntegrationService {

    private final IntegrationConfigRepository configRepository;
    private final LeadRepository leadRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Runs every 10 minutes (600000 ms)
    @Scheduled(fixedRate = 600000)
    public void fetchTradeIndiaLeads() {
        List<IntegrationConfig> activeConfigs = configRepository.findByNameAndEnabledTrue("TRADEINDIA");
        
        for (IntegrationConfig config : activeConfigs) {
            try {
                if (config.getApiKey() == null || config.getApiKey().isEmpty()) continue;
                
                log.info("Fetching TradeIndia leads for Admin ID {}", config.getUserIdFk());
                
                // TODO: Implement actual TradeIndia REST calls here once API docs are provided
                // String url = config.getApiUrl() + "?key=" + config.getApiKey();
                // String response = restTemplate.getForObject(url, String.class);
                // Parse JSON and map to Lead entity (similar to IndiamartIntegrationService)

            } catch (Exception e) {
                log.error("Error running TradeIndia integration for config {}: {}", config.getId(), e.getMessage());
                config.setSyncStatus("FAILURE");
                configRepository.save(config);
            }
        }
    }
}
