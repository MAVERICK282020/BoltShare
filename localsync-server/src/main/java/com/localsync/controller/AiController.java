package com.localsync.controller;

import com.localsync.dto.AiInsightDto;
import com.localsync.dto.DuplicateGroupDto;
import com.localsync.dto.StorageReportDto;
import com.localsync.service.AiEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AiController {

    private final AiEngineService aiEngineService;

    /**
     * Full storage analysis for a path.
     * GET /api/ai/analyze?path=C:/Users/aksha
     */
    @GetMapping("/analyze")
    public ResponseEntity<StorageReportDto> analyzeStorage(@RequestParam String path) {
        try {
            return ResponseEntity.ok(aiEngineService.analyzeStorage(path));
        } catch (Exception e) {
            log.error("Storage analysis failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Find duplicate files (SHA-256 based).
     * GET /api/ai/duplicates?path=C:/Users/aksha/Pictures
     */
    @GetMapping("/duplicates")
    public ResponseEntity<List<DuplicateGroupDto>> findDuplicates(@RequestParam String path) {
        try {
            return ResponseEntity.ok(aiEngineService.findDuplicates(path));
        } catch (Exception e) {
            log.error("Duplicate detection failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Find large files not accessed recently.
     * GET /api/ai/unused?path=...&days=30
     */
    @GetMapping("/unused")
    public ResponseEntity<List<Map<String, Object>>> findUnused(
            @RequestParam String path,
            @RequestParam(defaultValue = "30") int days) {
        try {
            return ResponseEntity.ok(aiEngineService.findUnusedFiles(path, days));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get AI insights and recommendations.
     * GET /api/ai/insights?path=...
     */
    @GetMapping("/insights")
    public ResponseEntity<List<AiInsightDto>> getInsights(@RequestParam String path) {
        try {
            return ResponseEntity.ok(aiEngineService.generateInsights(path));
        } catch (Exception e) {
            log.error("Insights generation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
