package com.smartcontainer.service;

import com.smartcontainer.entity.Container;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * FeatureEngineeringService — port of featureEngineering.js
 * Computes derived features from raw container shipment data.
 */
@Service
@Slf4j
public class FeatureEngineeringService {

    public static final double HIGH_DWELL_TIME_THRESHOLD = 72.0;

    /**
     * Engineer features for a single container record, given frequency maps.
     */
    public Container engineerFeatures(Container c,
                                       Map<String, Integer> importerFreqMap,
                                       Map<String, Integer> exporterFreqMap,
                                       Map<String, Double> tradeRouteRiskMap) {
        double declaredWeight = c.getDeclaredWeight() != null ? c.getDeclaredWeight() : 0.0;
        double measuredWeight = c.getMeasuredWeight() != null ? c.getMeasuredWeight() : 0.0;
        double declaredValue = c.getDeclaredValue() != null ? c.getDeclaredValue() : 0.0;
        double dwellTime = c.getDwellTimeHours() != null ? c.getDwellTimeHours() : 0.0;

        // Weight difference (absolute)
        c.setWeightDifference(Math.abs(measuredWeight - declaredWeight));

        // Weight mismatch percentage — capped at 200%
        c.setWeightMismatchPercentage(Math.min(
                (c.getWeightDifference() / (declaredWeight + 0.001)) * 100.0,
                200.0
        ));

        // Value to weight ratio
        double referenceWeight = measuredWeight > 0 ? measuredWeight : declaredWeight;
        c.setValueToWeightRatio(referenceWeight > 0 ? declaredValue / referenceWeight : 0.0);

        // High dwell time flag
        c.setHighDwellTimeFlag(dwellTime > HIGH_DWELL_TIME_THRESHOLD);

        // Importer frequency
        c.setImporterFrequency(importerFreqMap.getOrDefault(c.getImporterId(), 1));

        // Exporter frequency
        c.setExporterFrequency(exporterFreqMap.getOrDefault(c.getExporterId(), 1));

        // Trade route risk
        String routeKey = c.getOriginCountry() + "->" + c.getDestinationCountry();
        c.setTradeRouteRisk(tradeRouteRiskMap.getOrDefault(routeKey, 0.0));

        return c;
    }

    /**
     * Build frequency maps from a batch of records.
     */
    public Map<String, Object> buildFrequencyMaps(List<Container> records) {
        Map<String, Integer> importerFreqMap = new HashMap<>();
        Map<String, Integer> exporterFreqMap = new HashMap<>();
        Map<String, Integer> tradeRouteCountMap = new HashMap<>();

        for (Container rec : records) {
            if (rec.getImporterId() != null) {
                importerFreqMap.merge(rec.getImporterId(), 1, Integer::sum);
            }
            if (rec.getExporterId() != null) {
                exporterFreqMap.merge(rec.getExporterId(), 1, Integer::sum);
            }
            String routeKey = rec.getOriginCountry() + "->" + rec.getDestinationCountry();
            tradeRouteCountMap.merge(routeKey, 1, Integer::sum);
        }

        // Normalize trade route count into 0-1 risk score (rare = higher risk)
        int maxRouteCount = tradeRouteCountMap.values().stream().max(Integer::compareTo).orElse(1);
        Map<String, Double> tradeRouteRiskMap = new HashMap<>();
        for (Map.Entry<String, Integer> entry : tradeRouteCountMap.entrySet()) {
            tradeRouteRiskMap.put(entry.getKey(), 1.0 - (double) entry.getValue() / maxRouteCount);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("importerFreqMap", importerFreqMap);
        result.put("exporterFreqMap", exporterFreqMap);
        result.put("tradeRouteRiskMap", tradeRouteRiskMap);
        return result;
    }

    /**
     * Apply feature engineering to an entire batch of containers.
     */
    @SuppressWarnings("unchecked")
    public List<Container> engineerBatchFeatures(List<Container> records) {
        Map<String, Object> maps = buildFrequencyMaps(records);
        Map<String, Integer> importerFreqMap = (Map<String, Integer>) maps.get("importerFreqMap");
        Map<String, Integer> exporterFreqMap = (Map<String, Integer>) maps.get("exporterFreqMap");
        Map<String, Double> tradeRouteRiskMap = (Map<String, Double>) maps.get("tradeRouteRiskMap");

        for (Container rec : records) {
            engineerFeatures(rec, importerFreqMap, exporterFreqMap, tradeRouteRiskMap);
        }
        return records;
    }
}
