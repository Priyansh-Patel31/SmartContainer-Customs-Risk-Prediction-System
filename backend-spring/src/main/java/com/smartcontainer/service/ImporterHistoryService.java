package com.smartcontainer.service;

import com.smartcontainer.entity.Container;
import com.smartcontainer.repository.ContainerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ImporterHistoryService — port of importerHistoryService.js
 * Analyzes importer history to auto-escalate container risk levels.
 */
@Service
@RequiredArgsConstructor
public class ImporterHistoryService {

    private final ContainerRepository containerRepository;

    public Map<String, Object> getImporterStats(String importerId) {
        long totalShipments = containerRepository.countByImporterId(importerId);
        long criticalShipments = containerRepository.countByImporterIdAndRiskLevel(importerId, "Critical");
        
        double criticalPercentage = 0.0;
        if (totalShipments >= 5) {
            criticalPercentage = (double) criticalShipments / totalShipments * 100.0;
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalShipments", totalShipments);
        stats.put("criticalShipments", criticalShipments);
        stats.put("criticalPercentage", criticalPercentage);
        return stats;
    }

    public Map<String, Map<String, Object>> getBatchImporterStats(List<String> importerIds) {
        // In a real scenario with many IDs, you'd want a bulk query. 
        // For simplicity and matching the Node.js approach, we iterate or assume smaller batches.
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (String importerId : importerIds) {
            if (importerId != null && !result.containsKey(importerId)) {
                result.put(importerId, getImporterStats(importerId));
            }
        }
        return result;
    }

    public Container applyAutoEscalation(Container baseResult, Map<String, Object> importerStats) {
        double criticalPercentage = (Double) importerStats.getOrDefault("criticalPercentage", 0.0);
        long totalShipments = (Long) importerStats.getOrDefault("totalShipments", 0L);

        // Track both model risk and final risk
        baseResult.setModelRiskScore(baseResult.getRiskScore());
        baseResult.setModelRiskLevel(baseResult.getRiskLevel());
        baseResult.setFinalRiskScore(baseResult.getRiskScore());
        baseResult.setFinalRiskLevel(baseResult.getRiskLevel());
        baseResult.setAutoEscalatedByImporterHistory(false);
        baseResult.setImporterCriticalPercentage(criticalPercentage);
        baseResult.setExplanationSummary(baseResult.getExplanation());

        // Feature 7 Rule: If importer has >= 5 shipments and >30% were Critical, 
        // force escalate any 'Clear' or 'Low Risk' to 'Critical'
        if (totalShipments >= 5 && criticalPercentage >= 30.0) {
            if (!"Critical".equals(baseResult.getRiskLevel())) {
                baseResult.setFinalRiskLevel("Critical");
                // Artificially bump score to threshold if it's below
                if (baseResult.getRiskScore() != null && baseResult.getRiskScore() < 0.45) {
                     baseResult.setFinalRiskScore(0.46); // Just above Critical threshold
                }
                baseResult.setAutoEscalatedByImporterHistory(true);
                
                String overrideReason = String.format("Auto-escalated to Critical due to importer's severe risk history: %.1f%% of %d previous shipments were Critical.", 
                                        criticalPercentage, totalShipments);
                baseResult.setOverrideReason(overrideReason);
                
                String newExplanation = baseResult.getExplanation() + " " + overrideReason;
                baseResult.setExplanation(newExplanation);
                baseResult.setExplanationSummary(newExplanation);
            }
        }

        return baseResult;
    }
}
