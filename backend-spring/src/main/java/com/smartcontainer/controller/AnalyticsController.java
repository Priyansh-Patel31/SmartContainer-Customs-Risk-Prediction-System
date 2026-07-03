package com.smartcontainer.controller;

import com.smartcontainer.dto.ApiResponse;
import com.smartcontainer.repository.ContainerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Analytics Controller — Maps to remaining analytics routes from dashboardRoutes.js
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final ContainerRepository containerRepository;

    @GetMapping("/route-risk")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRouteRisk(@RequestParam(defaultValue = "20") int limit) {
        List<Object[]> results = containerRepository.getRouteRiskAnalytics(PageRequest.of(0, limit));
        List<Map<String, Object>> routes = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> item = new HashMap<>();
            item.put("origin", row[0]);
            item.put("destination", row[1]);
            item.put("total_shipments", row[2]);
            item.put("critical_count", row[3]);
            item.put("low_risk_count", row[4]);
            item.put("clear_count", row[5]);
            item.put("anomaly_count", row[6]);
            item.put("avg_risk_score", row[7]);
            item.put("avg_dwell_time", row[8]);
            routes.add(item);
        }
        return ResponseEntity.ok(ApiResponse.ok(routes));
    }

    @GetMapping("/suspicious-importers")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSuspiciousImporters(@RequestParam(defaultValue = "15") int limit) {
        List<Object[]> results = containerRepository.getSuspiciousImporters(PageRequest.of(0, limit));
        List<Map<String, Object>> importers = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> item = new HashMap<>();
            item.put("importer_id", row[0]);
            item.put("total_shipments", row[1]);
            item.put("critical_count", row[2]);
            item.put("anomaly_count", row[3]);
            item.put("avg_risk_score", row[4]);
            importers.add(item);
        }
        return ResponseEntity.ok(ApiResponse.ok(importers));
    }

    @GetMapping("/fraud-patterns")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFraudPatterns() {
        List<Object[]> hsCodes = containerRepository.getFraudHsCodes(PageRequest.of(0, 10));
        List<Map<String, Object>> topHs = new ArrayList<>();
        for (Object[] row : hsCodes) {
            Map<String, Object> item = Map.of("hs_code", row[0], "total", row[1], "critical", row[2], "avg_risk", row[3]);
            topHs.add(item);
        }

        List<Object[]> ships = containerRepository.getFraudShippingLines(PageRequest.of(0, 10));
        List<Map<String, Object>> topShips = new ArrayList<>();
        for (Object[] row : ships) {
            Map<String, Object> item = Map.of("shipping_line", row[0], "total", row[1], "critical", row[2], "avg_risk", row[3]);
            topShips.add(item);
        }

        Map<String, Object> patterns = Map.of("high_risk_hs_codes", topHs, "high_risk_shipping_lines", topShips);
        return ResponseEntity.ok(ApiResponse.ok(patterns));
    }

    @GetMapping("/risk-trend")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRiskTrend(@RequestParam(defaultValue = "30") int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Object[]> results = containerRepository.getRiskTrend(since);
        
        // Group by date
        Map<String, Map<String, Object>> trendMap = new HashMap<>();
        for (Object[] row : results) {
            String date = row[0].toString();
            String level = row[1].toString();
            long count = ((Number) row[2]).longValue();
            
            trendMap.putIfAbsent(date, new HashMap<>(Map.of("date", date, "Critical", 0L, "Low Risk", 0L, "Clear", 0L)));
            trendMap.get(date).put(level, count);
        }
        
        List<Map<String, Object>> trend = new ArrayList<>(trendMap.values());
        trend.sort((a, b) -> ((String) a.get("date")).compareTo((String) b.get("date")));
        
        return ResponseEntity.ok(ApiResponse.ok(trend));
    }

    @GetMapping("/importer-risk-history")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getImporterRiskHistory(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") double min_pct) {
        
        List<Object[]> results = containerRepository.getImporterRiskHistory(PageRequest.of(0, limit));
        List<Map<String, Object>> history = new ArrayList<>();
        for (Object[] row : results) {
            long total = ((Number) row[1]).longValue();
            long critical = ((Number) row[2]).longValue();
            double pct = total > 0 ? (double) critical / total * 100 : 0;
            
            if (pct >= min_pct) {
                Map<String, Object> item = new HashMap<>();
                item.put("importer_id", row[0]);
                item.put("total_shipments", total);
                item.put("critical_shipments", critical);
                item.put("auto_escalated_count", row[3]);
                item.put("avg_risk_score", row[4]);
                item.put("last_processed_at", row[5]);
                history.add(item);
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    @GetMapping("/escalation-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEscalationStats() {
        long totalAssessed = containerRepository.countByRiskLevelIsNotNull();
        long totalEscalated = containerRepository.countByAutoEscalatedByImporterHistoryTrue();
        
        List<Object[]> byImporter = containerRepository.getEscalationByImporter(PageRequest.of(0, 10));
        List<Map<String, Object>> topImporters = new ArrayList<>();
        for (Object[] row : byImporter) {
            topImporters.add(Map.of("importer_id", row[0], "escalated_count", row[1], "avg_critical_pct", row[2]));
        }
        
        Map<String, Object> stats = Map.of(
            "total_assessed", totalAssessed,
            "total_auto_escalated", totalEscalated,
            "escalation_rate_pct", totalAssessed > 0 ? (double) totalEscalated / totalAssessed * 100 : 0,
            "top_escalated_importers", topImporters
        );
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}
