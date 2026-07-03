package com.smartcontainer.controller;

import com.smartcontainer.dto.ApiResponse;
import com.smartcontainer.entity.Container;
import com.smartcontainer.entity.User;
import com.smartcontainer.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Dashboard Controller — Maps to routes in dashboardRoutes.js
 */
@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/api/summary")
    public ResponseEntity<Map<String, Object>> getSummary(@RequestAttribute("currentUser") User currentUser) {
        Map<String, Object> summary = dashboardService.getSummary(currentUser);
        summary.put("success", true);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/api/dashboard/risk-distribution")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRiskDistribution(@RequestAttribute("currentUser") User currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getRiskDistribution(currentUser)));
    }

    @GetMapping("/api/dashboard/top-risky-routes")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopRiskyRoutes() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getTopRiskyRoutes()));
    }

    @GetMapping("/api/dashboard/anomaly-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnomalyStats() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getAnomalyStats()));
    }

    @GetMapping("/api/dashboard/recent-high-risk")
    public ResponseEntity<ApiResponse<List<Container>>> getRecentHighRisk(@RequestAttribute("currentUser") User currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getRecentHighRisk(currentUser)));
    }

    @GetMapping("/api/dashboard/containers")
    public ResponseEntity<Map<String, Object>> getContainersList(
            @RequestAttribute("currentUser") User currentUser,
            @RequestParam(required = false) String risk_level,
            @RequestParam(required = false) Boolean anomaly,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int limit) {

        Page<Container> pagedResult = dashboardService.getContainersList(currentUser, risk_level, anomaly, page, limit);

        Map<String, Object> response = Map.of(
                "success", true,
                "data", pagedResult.getContent(),
                "total", pagedResult.getTotalElements(),
                "page", page,
                "limit", limit
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/containers/all")
    public ResponseEntity<ApiResponse<Void>> clearAllData(@RequestAttribute("currentUser") User currentUser) {
        dashboardService.clearAllData(currentUser);
        return ResponseEntity.ok(ApiResponse.ok("All container data cleared."));
    }

    @GetMapping("/api/container-location/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getContainerLocation(@PathVariable String id, com.smartcontainer.service.TrackingService trackingService) {
        return ResponseEntity.ok(ApiResponse.ok(trackingService.getTrack(id)));
    }

    @GetMapping("/api/container-route/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getContainerRoute(@PathVariable String id, com.smartcontainer.service.TrackingService trackingService) {
        return ResponseEntity.ok(ApiResponse.ok(trackingService.getTrack(id)));
    }

    @GetMapping("/api/container-analysis/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getContainerAnalysis(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("container_id", id, "status", "Analysis complete", "risk_factors", List.of())));
    }

    @GetMapping("/api/container-timeline/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getContainerTimeline(@PathVariable String id, com.smartcontainer.service.TrackingService trackingService) {
        return ResponseEntity.ok(ApiResponse.ok(trackingService.getTrack(id)));
    }
}
