package com.smartcontainer.service;

import com.smartcontainer.entity.Container;
import com.smartcontainer.repository.ContainerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PredictionService — Orchestrates feature engineering → ML service → risk classification → persistence.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PredictionService {

    private final ContainerRepository containerRepository;
    private final FeatureEngineeringService featureEngineeringService;
    private final MLClientService mlClientService;
    private final RiskClassifierService riskClassifierService;
    private final ImporterHistoryService importerHistoryService;

    public Map<String, Object> predictSingle(Container rawRecord) {
        // Build frequency maps
        long importerCount = containerRepository.countByImporterId(rawRecord.getImporterId());
        long exporterCount = containerRepository.countByExporterId(rawRecord.getExporterId());
        
        Map<String, Integer> importerFreqMap = Map.of(rawRecord.getImporterId(), Math.max(1, (int) importerCount));
        Map<String, Integer> exporterFreqMap = Map.of(rawRecord.getExporterId(), Math.max(1, (int) exporterCount));

        // Trade route risk
        // In a real app, you'd calculate this from DB. Stubbing for simplicity matching Node.
        Map<String, Double> tradeRouteRiskMap = new HashMap<>();

        // Engineer Features
        Container enriched = featureEngineeringService.engineerFeatures(rawRecord, importerFreqMap, exporterFreqMap, tradeRouteRiskMap);

        // Call ML Service
        Map<String, Object> mlResult = mlClientService.predict(enriched);
        Double riskScore = (Double) mlResult.get("risk_score");
        Boolean anomalyFlag = (Boolean) mlResult.get("anomaly_flag");
        Double anomalyScore = (Double) mlResult.get("anomaly_score");

        // Classify
        Map<String, Object> classification = riskClassifierService.classifyAndExplain(riskScore, enriched);
        
        enriched.setRiskScore(riskScore);
        enriched.setRiskLevel((String) classification.get("risk_level"));
        enriched.setAnomalyFlag(anomalyFlag);
        enriched.setAnomalyScore(anomalyScore);
        enriched.setExplanation((String) classification.get("explanation"));
        
        // Auto-escalation
        Map<String, Object> importerStats = importerHistoryService.getImporterStats(enriched.getImporterId());
        Container escalated = importerHistoryService.applyAutoEscalation(enriched, importerStats);
        
        escalated.setPredictionSource("single");
        escalated.setProcessedAt(LocalDateTime.now());

        // Upsert
        Container saved = containerRepository.findByContainerId(escalated.getContainerId())
                .map(existing -> {
                    escalated.setId(existing.getId());
                    return containerRepository.save(escalated);
                })
                .orElseGet(() -> containerRepository.save(escalated));

        Map<String, Object> response = new HashMap<>();
        response.put("container_id", saved.getContainerId());
        response.put("risk_score", saved.getRiskScore());
        response.put("risk_level", saved.getRiskLevel());
        response.put("anomaly_flag", saved.getAnomalyFlag());
        response.put("anomaly_score", saved.getAnomalyScore());
        response.put("explanation", saved.getExplanation());
        response.put("inspection_recommendation", classification.get("inspection_recommendation"));
        
        Map<String, Object> impStatsResponse = new HashMap<>();
        impStatsResponse.put("total_shipments", importerStats.get("totalShipments"));
        impStatsResponse.put("critical_shipments", importerStats.get("criticalShipments"));
        impStatsResponse.put("critical_percentage", importerStats.get("criticalPercentage"));
        response.put("importer_stats", impStatsResponse);
        
        return response;
    }

    public Map<String, Object> predictBatch(List<Container> rawRecords, String batchId) {
        List<Container> enrichedRecords = featureEngineeringService.engineerBatchFeatures(rawRecords);

        Map<String, Object> mlResponse = mlClientService.predictBatch(enrichedRecords);
        List<Map<String, Object>> mlResults = (List<Map<String, Object>>) mlResponse.get("predictions");

        List<String> importerIds = enrichedRecords.stream().map(Container::getImporterId).distinct().collect(Collectors.toList());
        Map<String, Map<String, Object>> importerStatsMap = importerHistoryService.getBatchImporterStats(importerIds);

        List<Map<String, Object>> results = new ArrayList<>();
        List<Container> toSave = new ArrayList<>();
        int autoEscalatedCount = 0;
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < enrichedRecords.size(); i++) {
            Container enriched = enrichedRecords.get(i);
            Map<String, Object> ml = mlResults.size() > i ? mlResults.get(i) : new HashMap<>();
            
            Double riskScore = (Double) ml.getOrDefault("risk_score", 0.0);
            Boolean anomalyFlag = (Boolean) ml.getOrDefault("anomaly_flag", false);
            Double anomalyScore = (Double) ml.getOrDefault("anomaly_score", 0.0);

            Map<String, Object> classification = riskClassifierService.classifyAndExplain(riskScore, enriched);

            enriched.setRiskScore(riskScore);
            enriched.setRiskLevel((String) classification.get("risk_level"));
            enriched.setAnomalyFlag(anomalyFlag);
            enriched.setAnomalyScore(anomalyScore);
            enriched.setExplanation((String) classification.get("explanation"));

            Map<String, Object> importerStats = importerStatsMap.getOrDefault(enriched.getImporterId(), new HashMap<>());
            Container escalated = importerHistoryService.applyAutoEscalation(enriched, importerStats);

            if (Boolean.TRUE.equals(escalated.getAutoEscalatedByImporterHistory())) {
                autoEscalatedCount++;
            }

            escalated.setPredictionSource("batch");
            escalated.setUploadBatchId(batchId);
            escalated.setProcessedAt(now);

            // In a real scenario, use JDBC batch inserts for performance. 
            // Using JPA saveAll here for simplicity.
            toSave.add(escalated);
            
            Map<String, Object> resultItem = new HashMap<>();
            resultItem.put("container_id", escalated.getContainerId());
            resultItem.put("risk_score", escalated.getRiskScore());
            resultItem.put("risk_level", escalated.getRiskLevel());
            resultItem.put("anomaly_flag", escalated.getAnomalyFlag());
            resultItem.put("explanation", escalated.getExplanation());
            results.add(resultItem);
        }

        // We should really handle upserts properly here instead of blind saveAll
        // But for this prototype, we'll try to find existing ones first
        for (Container c : toSave) {
            containerRepository.findByContainerId(c.getContainerId()).ifPresent(existing -> c.setId(existing.getId()));
        }
        containerRepository.saveAll(toSave);

        Map<String, Object> finalResponse = new HashMap<>();
        finalResponse.put("results", results);
        finalResponse.put("autoEscalatedCount", autoEscalatedCount);
        return finalResponse;
    }

    public Map<String, Object> triggerTraining() {
        return mlClientService.train();
    }
}
