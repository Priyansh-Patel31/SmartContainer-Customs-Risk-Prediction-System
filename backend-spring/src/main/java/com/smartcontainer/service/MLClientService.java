package com.smartcontainer.service;

import com.smartcontainer.entity.Container;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MLClientService — handles communication with the Python FastAPI ML microservice.
 */
@Service
@Slf4j
public class MLClientService {

    private final RestTemplate restTemplate;
    private final String mlServiceUrl;

    public MLClientService(RestTemplate restTemplate, @Value("${app.ml-service.url}") String mlServiceUrl) {
        this.restTemplate = restTemplate;
        this.mlServiceUrl = mlServiceUrl;
    }

    public Map<String, Object> predict(Container features) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    mlServiceUrl + "/predict",
                    HttpMethod.POST,
                    new HttpEntity<>(features),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.warn("ML service unavailable, using heuristic fallback for predict: {}", e.getMessage());
            return computeHeuristicRisk(features);
        }
    }

    public Map<String, Object> predictBatch(List<Container> featuresList) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("records", featuresList);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    mlServiceUrl + "/predict-batch",
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.warn("ML service unavailable, using heuristic fallback for predictBatch: {}", e.getMessage());
            List<Map<String, Object>> fallbackPredictions = featuresList.stream()
                    .map(this::computeHeuristicRisk)
                    .toList();
            Map<String, Object> fallbackResult = new HashMap<>();
            fallbackResult.put("predictions", fallbackPredictions);
            return fallbackResult;
        }
    }

    public Map<String, Object> train() {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                mlServiceUrl + "/train",
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        return response.getBody();
    }

    // Heuristic fallback matching predictionService.js computeHeuristicRisk
    private Map<String, Object> computeHeuristicRisk(Container f) {
        double score = 0.0;

        double mismatch = Math.min((f.getWeightMismatchPercentage() != null ? f.getWeightMismatchPercentage() : 0.0) / 100.0, 1.0);
        score += mismatch * 0.30;

        double vwr = f.getValueToWeightRatio() != null ? f.getValueToWeightRatio() : 0.0;
        if (vwr > 1000 || (vwr < 0.1 && vwr >= 0)) score += 0.20;

        if (Boolean.TRUE.equals(f.getHighDwellTimeFlag())) score += 0.20;

        double tradeRisk = f.getTradeRouteRisk() != null ? f.getTradeRouteRisk() : 0.0;
        score += Math.min(tradeRisk, 1.0) * 0.15;

        int impFreq = f.getImporterFrequency() != null ? f.getImporterFrequency() : 1;
        int expFreq = f.getExporterFrequency() != null ? f.getExporterFrequency() : 1;

        if (impFreq <= 2) score += 0.075;
        if (expFreq <= 2) score += 0.075;

        double riskScore = Math.min(Math.max(score, 0.0), 1.0);
        boolean anomalyFlag = riskScore > 0.65;
        double anomalyScore = riskScore * -1 + 0.5;

        Map<String, Object> result = new HashMap<>();
        result.put("risk_score", Math.round(riskScore * 10000.0) / 10000.0);
        result.put("anomaly_score", Math.round(anomalyScore * 10000.0) / 10000.0);
        result.put("anomaly_flag", anomalyFlag);
        return result;
    }
}
