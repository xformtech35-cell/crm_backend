package com.crm.controller;

import com.crm.dto.response.ApiResponse;
import com.crm.dto.response.LeadScoreResponse;
import com.crm.service.LeadScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
public class LeadScoreController {

    private final LeadScoringService leadScoringService;

    /**
     * GET /api/leads/{id}/score
     * Compute (or refresh) and return the AI score for a single lead.
     */
    @GetMapping("/{id}/score")
    public ResponseEntity<ApiResponse<LeadScoreResponse>> getScore(@PathVariable Long id) {
        LeadScoreResponse response = leadScoringService.scoreAndCache(id);
        return ResponseEntity.ok(ApiResponse.success("Lead score calculated", response));
    }

    /**
     * GET /api/leads/scores
     * Returns all lead scores sorted by score descending (highest first).
     */
    @GetMapping("/scores")
    public ResponseEntity<ApiResponse<List<LeadScoreResponse>>> getAllScores() {
        List<LeadScoreResponse> scores = leadScoringService.getAllScores();
        return ResponseEntity.ok(ApiResponse.success("Lead scores fetched", scores));
    }

    /**
     * POST /api/leads/scores/recalculate
     * Triggers an async bulk recalculation of scores for every lead.
     * Returns immediately — processing happens in the background.
     */
    @PostMapping("/scores/recalculate")
    public ResponseEntity<ApiResponse<Void>> recalculateAll() {
        leadScoringService.recalculateAll();
        return ResponseEntity.ok(ApiResponse.success("Bulk recalculation started", null));
    }
}
