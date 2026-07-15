package com.crm.service;

import com.crm.dto.response.LeadScoreResponse;
import com.crm.entity.Lead;
import com.crm.entity.LeadScore;
import com.crm.entity.LeadScore.Grade;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.LeadNoteRepository;
import com.crm.repository.LeadReminderRepository;
import com.crm.repository.LeadRepository;
import com.crm.repository.LeadScoreRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadScoringService {

    private final LeadRepository         leadRepository;
    private final LeadScoreRepository    leadScoreRepository;
    private final LeadNoteRepository     leadNoteRepository;
    private final LeadReminderRepository leadReminderRepository;
    private final ObjectMapper           objectMapper;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Compute (or refresh) the score for a single lead and persist it.
     */
    @Transactional
    public LeadScoreResponse scoreAndCache(Long leadId) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead", "id", leadId));
        return buildAndPersist(lead);
    }

    /**
     * Returns ALL persisted scores sorted descending.
     * Leads that have never been scored are scored on the fly so the list
     * is always complete.
     */
    @Transactional
    public List<LeadScoreResponse> getAllScores() {
        // Score any leads that are missing a cached entry
        List<Long> scoredIds = leadScoreRepository.findAll()
                .stream().map(LeadScore::getLeadIdFk).collect(Collectors.toList());
        leadRepository.findAll().stream()
                .filter(l -> !scoredIds.contains(l.getLeadId()))
                .forEach(this::buildAndPersist);

        return leadScoreRepository.findAllByOrderByScoreDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Async bulk recalculation of every lead in the system.
     */
    @Async
    @Transactional
    public void recalculateAll() {
        log.info("LeadScoringService: starting bulk recalculation");
        List<Lead> all = leadRepository.findAll();
        for (Lead lead : all) {
            try {
                buildAndPersist(lead);
            } catch (Exception e) {
                log.warn("Failed to score lead {}: {}", lead.getLeadId(), e.getMessage());
            }
        }
        log.info("LeadScoringService: bulk recalculation complete — {} leads processed", all.size());
    }

    // -------------------------------------------------------------------------
    // Scoring logic
    // -------------------------------------------------------------------------

    /**
     * Calculates the raw weighted score, normalises to 0-100, derives the
     * grade, persists (upsert) and returns the response DTO.
     */
    private LeadScoreResponse buildAndPersist(Lead lead) {
        List<String> factors = new ArrayList<>();
        int raw = 0;

        // --- Contact completeness (max 35) ---
        if (isPresent(lead.getLeadEmail())) {
            raw += 15;
            factors.add("Has email (+15)");
        }
        if (isPresent(lead.getLeadMobileNo())) {
            raw += 10;
            factors.add("Has mobile (+10)");
        }
        if (isPresent(lead.getLeadOrganisationName())) {
            raw += 10;
            factors.add("Has organisation (+10)");
        }

        // --- Source (max 20) ---
        String source = lead.getLeadSource();
        if ("Indiamart".equalsIgnoreCase(source)) {
            raw += 20;
            factors.add("Source: Indiamart (+20)");
        } else if ("Referral".equalsIgnoreCase(source)) {
            raw += 15;
            factors.add("Source: Referral (+15)");
        } else if ("Website".equalsIgnoreCase(source)) {
            raw += 10;
            factors.add("Source: Website (+10)");
        }

        // --- Status (max 25) ---
        String status = lead.getLeadStatus();
        if ("Qualified Lead".equalsIgnoreCase(status)) {
            raw += 25;
            factors.add("Status: Qualified Lead (+25)");
        } else if ("Working".equalsIgnoreCase(status)) {
            raw += 15;
            factors.add("Status: Working (+15)");
        } else if ("Contacted".equalsIgnoreCase(status)) {
            raw += 5;
            factors.add("Status: Contacted (+5)");
        }

        // --- Engagement signals (max 22 = 5 + 5 + 12) ---
        long noteCount     = leadNoteRepository.countByLeadIdFk(lead.getLeadId());
        long reminderCount = leadReminderRepository.countByLeadIdFk(lead.getLeadId());

        int  docCount      = countDocuments(lead);

        if (noteCount > 0) {
            raw += 5;
            factors.add("Has notes (+5)");
        }
        if (reminderCount > 0) {
            raw += 5;
            factors.add("Has reminders (+5)");
        }
        int docPoints = Math.min(docCount * 3, 12);
        if (docPoints > 0) {
            raw += docPoints;
            factors.add("Documents: " + docCount + " (+" + docPoints + ")");
        }

        // --- Normalise to 0-100 ---
        // Max possible raw = 35 + 20 + 25 + 22 = 102 → clamp then scale
        int normalised = (int) Math.round(Math.min(raw, 102) * 100.0 / 102.0);
        normalised = Math.max(0, Math.min(100, normalised));

        Grade grade = gradeFor(normalised);

        // Keep only the top 5 factors for storage
        List<String> topFactors = factors.stream().limit(5).collect(Collectors.toList());

        // Upsert *only if changed*
        String topFactorsJson = toJson(topFactors);
        LocalDateTime now = LocalDateTime.now();

        LeadScore existing = leadScoreRepository.findByLeadIdFk(lead.getLeadId()).orElse(null);
        boolean changed = existing == null
                || existing.getScore() == null || existing.getScore() != normalised
                || existing.getGrade() == null || !existing.getGrade().equals(grade)
                || existing.getTopFactors() == null || !existing.getTopFactors().equals(topFactorsJson);

        if (changed) {
            LeadScore entity = existing != null ? existing
                    : LeadScore.builder().leadIdFk(lead.getLeadId()).build();

            entity.setScore(normalised);
            entity.setGrade(grade);
            entity.setTopFactors(topFactorsJson);
            entity.setCalculatedAt(now);
            leadScoreRepository.save(entity);

            existing = entity;
        }

        // If unchanged, avoid DB write; still return computed response
        LocalDateTime calculatedAt = existing != null && existing.getCalculatedAt() != null
                ? existing.getCalculatedAt()
                : now;

        return toResponse(lead, normalised, grade, topFactors, calculatedAt);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private static int countDocuments(Lead lead) {
        int count = 0;
        if (isPresent(lead.getUploadDocument()))  count++;
        if (isPresent(lead.getUploadDocument1())) count++;
        if (isPresent(lead.getUploadDocument2())) count++;
        if (isPresent(lead.getUploadDocument3())) count++;
        return count;
    }

    private static Grade gradeFor(int score) {
        if (score >= 80) return Grade.A;
        if (score >= 60) return Grade.B;
        if (score >= 40) return Grade.C;    
        return Grade.D;
    }

    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    /** Map a persisted LeadScore → response (looks up lead name). */
    private LeadScoreResponse toResponse(LeadScore ls) {
        Lead lead = leadRepository.findById(ls.getLeadIdFk()).orElse(null);
        String name = lead == null ? "Unknown"
                : (lead.getLeadFirstName() + " " + lead.getLeadLastName()).trim();
        return LeadScoreResponse.builder()
                .leadId(ls.getLeadIdFk())
                .leadName(name)
                .score(ls.getScore())
                .grade(ls.getGrade())
                .topFactors(fromJson(ls.getTopFactors()))
                .calculatedAt(ls.getCalculatedAt())
                .build();
    }

    /** Build response directly from components (avoids a second DB read). */
    private LeadScoreResponse toResponse(Lead lead, int score, Grade grade,
                                         List<String> topFactors, LocalDateTime calculatedAt) {
        String name = (lead.getLeadFirstName() + " " + lead.getLeadLastName()).trim();
        return LeadScoreResponse.builder()
                .leadId(lead.getLeadId())
                .leadName(name)
                .score(score)
                .grade(grade)
                .topFactors(topFactors)
                .calculatedAt(calculatedAt)
                .build();
  }
}
