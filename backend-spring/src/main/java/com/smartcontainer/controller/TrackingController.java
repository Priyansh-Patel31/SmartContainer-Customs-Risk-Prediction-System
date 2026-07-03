package com.smartcontainer.controller;

import com.smartcontainer.dto.ApiResponse;
import com.smartcontainer.dto.RequestDTOs;
import com.smartcontainer.service.TrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Tracking Controller — Maps to routes in trackingRoutes.js
 */
@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    @GetMapping("/fleet")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFleet(
            @RequestParam(required = false) String risk_level,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(trackingService.getTracks(risk_level, status, limit)));
    }

    @GetMapping("/{containerId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getContainerTrack(@PathVariable String containerId) {
        return ResponseEntity.ok(ApiResponse.ok(trackingService.getTrack(containerId)));
    }

    @PostMapping("/link-vessel")
    public ResponseEntity<ApiResponse<Void>> linkVessel(@Valid @RequestBody RequestDTOs.LinkVesselRequest request) {
        trackingService.linkVessel(request.getContainer_id(), request.getVessel_imo(), request.getVessel_name());
        return ResponseEntity.ok(ApiResponse.ok("Vessel linked successfully"));
    }

    @PostMapping("/{containerId}/force-refresh")
    public ResponseEntity<ApiResponse<Void>> forceRefresh(@PathVariable String containerId) {
        trackingService.forceRefresh(containerId);
        return ResponseEntity.ok(ApiResponse.ok("Position refresh triggered"));
    }
}
