package com.smartcontainer.controller;

import com.smartcontainer.dto.ApiResponse;
import com.smartcontainer.service.ImporterHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Exporter Controller — Maps to routes in exporterRoutes.js (which mostly returns stub data for the frontend)
 */
@RestController
@RequestMapping("/api/exporters")
@RequiredArgsConstructor
public class ExporterController {

    private final ImporterHistoryService importerHistoryService;

    @GetMapping("/{exporterId}/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExporterStats(@PathVariable String exporterId) {
        // Exporter stats reuse importer stats logic as a stub since they are structurally identical
        Map<String, Object> stats = importerHistoryService.getImporterStats(exporterId);
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}
