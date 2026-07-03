package com.smartcontainer.service;

import com.smartcontainer.entity.Container;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * RiskClassifierService — port of riskClassifier.js
 * Classifies risk scores and generates human-readable explanations.
 */
@Service
@Slf4j
public class RiskClassifierService {

    // Thresholds (match Node.js defaults)
    public static final double CRITICAL_THRESHOLD = 0.45;
    public static final double LOW_RISK_THRESHOLD = 0.20;
    public static final double WEIGHT_MISMATCH_THRESHOLD = 20.0;
    public static final double VALUE_WEIGHT_HIGH_THRESHOLD = 1000.0;
    public static final double VALUE_WEIGHT_LOW_THRESHOLD = 0.1;
    public static final double DWELL_TIME_THRESHOLD = 72.0;
    public static final double WEIGHT_DIFF_THRESHOLD = 500.0;
    public static final double TRADE_ROUTE_RISK_THRESHOLD = 0.7;

    public String classifyRisk(double riskScore) {
        if (riskScore >= CRITICAL_THRESHOLD) return "Critical";
        if (riskScore >= LOW_RISK_THRESHOLD) return "Low Risk";
        return "Clear";
    }

    public String generateExplanation(Container features, double riskScore) {
        List<String> reasons = new ArrayList<>();
        double declaredWeight = features.getDeclaredWeight() != null ? features.getDeclaredWeight() : 0.0;

        // Zero declared weight
        if (declaredWeight == 0.0) {
            reasons.add("Declared weight is zero — possible evasion or missing manifest data.");
        }

        // Weight mismatch
        double mismatch = features.getWeightMismatchPercentage() != null ? features.getWeightMismatchPercentage() : 0.0;
        if (mismatch > WEIGHT_MISMATCH_THRESHOLD) {
            reasons.add(String.format("Measured weight differs from declared weight by %.1f%%.", mismatch));
        }

        // Absolute weight diff
        double weightDiff = features.getWeightDifference() != null ? features.getWeightDifference() : 0.0;
        if (weightDiff > WEIGHT_DIFF_THRESHOLD) {
            reasons.add(String.format("Large absolute weight discrepancy of %.0f kg detected.", weightDiff));
        }

        // High value-to-weight
        double vwr = features.getValueToWeightRatio() != null ? features.getValueToWeightRatio() : 0.0;
        if (vwr > VALUE_WEIGHT_HIGH_THRESHOLD) {
            reasons.add("Unusually high value-to-weight ratio detected.");
        }

        // Low value-to-weight
        if (vwr < VALUE_WEIGHT_LOW_THRESHOLD && vwr >= 0.0 && features.getValueToWeightRatio() != null) {
            reasons.add("Suspiciously low declared value relative to shipment weight.");
        }

        // Excessive dwell time
        double dwellTime = features.getDwellTimeHours() != null ? features.getDwellTimeHours() : 0.0;
        boolean dwellFlag = Boolean.TRUE.equals(features.getHighDwellTimeFlag()) || dwellTime > DWELL_TIME_THRESHOLD;
        if (dwellFlag) {
            reasons.add(String.format("Container dwell time is unusually high (%.0f hours).", dwellTime));
        }

        // Rare trade route
        double routeRisk = features.getTradeRouteRisk() != null ? features.getTradeRouteRisk() : 0.0;
        if (routeRisk > TRADE_ROUTE_RISK_THRESHOLD) {
            reasons.add(String.format("Trade route %s → %s has elevated risk frequency.",
                    features.getOriginCountry(), features.getDestinationCountry()));
        }

        // Low importer frequency
        int importerFreq = features.getImporterFrequency() != null ? features.getImporterFrequency() : 0;
        if (importerFreq <= 2) {
            reasons.add("Importer has very few recorded shipments — low historical activity.");
        }

        if (reasons.isEmpty()) {
            if (riskScore >= CRITICAL_THRESHOLD) {
                return "Multiple combined risk signals contribute to this critical classification.";
            }
            if (riskScore >= LOW_RISK_THRESHOLD) {
                return "Minor risk indicators detected. Further review recommended.";
            }
            return "No significant risk indicators detected. Shipment appears normal.";
        }

        return String.join(" ", reasons);
    }

    public Map<String, Object> recommendInspection(double riskScore, Container features) {
        String riskLevel = classifyRisk(riskScore);
        Map<String, Object> result = new HashMap<>();

        double declaredWeight = features.getDeclaredWeight() != null ? features.getDeclaredWeight() : 0.0;
        double mismatch = features.getWeightMismatchPercentage() != null ? features.getWeightMismatchPercentage() : 0.0;
        double vwr = features.getValueToWeightRatio() != null ? features.getValueToWeightRatio() : 0.0;
        double routeRisk = features.getTradeRouteRisk() != null ? features.getTradeRouteRisk() : 0.0;
        int importerFreq = features.getImporterFrequency() != null ? features.getImporterFrequency() : 1;
        boolean dwellFlag = Boolean.TRUE.equals(features.getHighDwellTimeFlag());

        if ("Critical".equals(riskLevel)) {
            if (declaredWeight == 0.0 || mismatch > 50) {
                result.put("recommendedAction", "Full Physical Inspection");
                result.put("reason", "Severe weight anomaly or zero declared weight suggests manifest fraud");
                result.put("confidence", "High");
            } else if (vwr > 1000) {
                result.put("recommendedAction", "X-Ray Scanning + Documentation Audit");
                result.put("reason", "Extreme value-to-weight ratio indicates possible high-value contraband");
                result.put("confidence", "High");
            } else if (dwellFlag) {
                result.put("recommendedAction", "Full Physical Inspection");
                result.put("reason", "Excessive port dwell time is a strong smuggling indicator");
                result.put("confidence", "High");
            } else if (routeRisk > 0.7) {
                result.put("recommendedAction", "X-Ray Scanning + Officer Review");
                result.put("reason", "High-risk trade route with elevated critical container history");
                result.put("confidence", "High");
            } else {
                result.put("recommendedAction", "X-Ray Scanning");
                result.put("reason", "Multiple combined risk signals exceed critical threshold");
                result.put("confidence", "High");
            }
        } else if ("Low Risk".equals(riskLevel)) {
            if (routeRisk > 0.5) {
                result.put("recommendedAction", "Documentation Audit");
                result.put("reason", "Elevated route risk warrants document verification");
                result.put("confidence", "Medium");
            } else if (importerFreq <= 2) {
                result.put("recommendedAction", "Documentation Audit");
                result.put("reason", "New or infrequent importer requires additional scrutiny");
                result.put("confidence", "Medium");
            } else {
                result.put("recommendedAction", "Random Spot Check");
                result.put("reason", "Minor risk indicators present — low priority review");
                result.put("confidence", "Low");
            }
        } else {
            result.put("recommendedAction", "Standard Processing");
            result.put("reason", "No significant risk indicators detected");
            result.put("confidence", "Low");
        }

        return result;
    }

    public Map<String, Object> classifyAndExplain(double riskScore, Container features) {
        Map<String, Object> result = new HashMap<>();
        result.put("risk_level", classifyRisk(riskScore));
        result.put("explanation", generateExplanation(features, riskScore));
        result.put("inspection_recommendation", recommendInspection(riskScore, features));
        return result;
    }
}
