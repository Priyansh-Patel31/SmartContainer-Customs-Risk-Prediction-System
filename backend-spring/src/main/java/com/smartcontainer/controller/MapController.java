package com.smartcontainer.controller;

import com.smartcontainer.dto.ApiResponse;
import com.smartcontainer.service.MapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Map Controller — Maps to routes in mapRoutes.js
 */
@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;

    @GetMapping("/all-routes")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRoutes() {
        return ResponseEntity.ok(ApiResponse.ok(mapService.getAllRoutes()));
    }

    @GetMapping("/heatmap")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getHeatmap() {
        return ResponseEntity.ok(ApiResponse.ok(mapService.getHeatmapData()));
    }

    @GetMapping("/tracks")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTracks(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String risk_level,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String status,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "100") int limit,
            com.smartcontainer.service.TrackingService trackingService) {
        return ResponseEntity.ok(ApiResponse.ok(trackingService.getTracks(risk_level, status, limit)));
    }

    @GetMapping("/track/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTrack(@org.springframework.web.bind.annotation.PathVariable String id, com.smartcontainer.service.TrackingService trackingService) {
        return ResponseEntity.ok(ApiResponse.ok(trackingService.getTrack(id)));
    }
}
