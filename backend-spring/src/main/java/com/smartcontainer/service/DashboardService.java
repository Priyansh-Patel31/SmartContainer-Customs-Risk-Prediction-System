package com.smartcontainer.service;

import com.smartcontainer.entity.Container;
import com.smartcontainer.entity.User;
import com.smartcontainer.repository.ContainerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DashboardService — fetches analytics and aggregations.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ContainerRepository containerRepository;
    private final AuditService auditService;

    public Map<String, Object> getSummary(User user) {
        boolean isExporter = "exporter".equals(user.getRole());
        String expId = String.valueOf(user.getId());

        long total = isExporter ? containerRepository.countByExporterId(expId) : containerRepository.count();
        long critical = isExporter ? containerRepository.countByExporterIdAndRiskLevel(expId, "Critical") : containerRepository.countByRiskLevel("Critical");
        long lowRisk = isExporter ? containerRepository.countByExporterIdAndRiskLevel(expId, "Low Risk") : containerRepository.countByRiskLevel("Low Risk");
        long clear = isExporter ? containerRepository.countByExporterIdAndRiskLevel(expId, "Clear") : containerRepository.countByRiskLevel("Clear");
        long anomalies = isExporter ? containerRepository.countByExporterIdAndAnomalyFlagTrue(expId) : containerRepository.countByAnomalyFlagTrue();
        long unprocessed = isExporter ? containerRepository.countByExporterIdAndRiskLevelIsNull(expId) : containerRepository.countByRiskLevelIsNull();

        Map<String, Object> summary = new HashMap<>();
        summary.put("total_containers", total);
        summary.put("critical_count", critical);
        summary.put("low_risk_count", lowRisk);
        summary.put("clear_count", clear);
        summary.put("anomaly_count", anomalies);
        summary.put("unprocessed_count", unprocessed);
        return summary;
    }

    public List<Map<String, Object>> getRiskDistribution(User user) {
        boolean isExporter = "exporter".equals(user.getRole());
        List<Object[]> results = isExporter ? 
                containerRepository.getRiskDistributionByExporter(String.valueOf(user.getId())) : 
                containerRepository.getRiskDistribution();

        List<Map<String, Object>> dist = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> item = new HashMap<>();
            item.put("level", row[0]);
            item.put("count", row[1]);
            dist.add(item);
        }
        return dist;
    }

    public List<Map<String, Object>> getTopRiskyRoutes() {
        List<Object[]> results = containerRepository.getTopRiskyRoutes(PageRequest.of(0, 10));
        List<Map<String, Object>> routes = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> item = new HashMap<>();
            item.put("origin", row[0]);
            item.put("destination", row[1]);
            item.put("critical_count", row[2]);
            item.put("avg_risk_score", row[3]);
            routes.add(item);
        }
        return routes;
    }

    public Map<String, Object> getAnomalyStats() {
        List<Object[]> byCountry = containerRepository.getAnomalyByCountry(PageRequest.of(0, 10));
        List<Map<String, Object>> countryStats = new ArrayList<>();
        for (Object[] row : byCountry) {
            Map<String, Object> item = new HashMap<>();
            item.put("country", row[0]);
            item.put("anomaly_count", row[1]);
            countryStats.add(item);
        }

        List<Object[]> byLevel = containerRepository.getAnomalyByRiskLevel();
        List<Map<String, Object>> levelStats = new ArrayList<>();
        for (Object[] row : byLevel) {
            Map<String, Object> item = new HashMap<>();
            item.put("risk_level", row[0]);
            item.put("anomaly_count", row[1]);
            levelStats.add(item);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("top_origin_countries", countryStats);
        stats.put("by_risk_level", levelStats);
        return stats;
    }

    public List<Container> getRecentHighRisk(User user) {
        boolean isExporter = "exporter".equals(user.getRole());
        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "processedAt"));
        
        if (isExporter) {
            return containerRepository.findByRiskLevelAndExporterIdOrderByProcessedAtDesc("Critical", String.valueOf(user.getId()), pageable);
        } else {
            return containerRepository.findByRiskLevelOrderByProcessedAtDesc("Critical", pageable);
        }
    }

    public Page<Container> getContainersList(User user, String riskLevel, Boolean anomaly, int page, int limit) {
        boolean isExporter = "exporter".equals(user.getRole());
        String expId = String.valueOf(user.getId());
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (isExporter) {
            if (riskLevel != null && !riskLevel.isEmpty()) {
                return containerRepository.findByExporterIdAndRiskLevel(expId, riskLevel, pageable);
            }
            return containerRepository.findByExporterId(expId, pageable);
        } else {
            if (riskLevel != null && !riskLevel.isEmpty() && Boolean.TRUE.equals(anomaly)) {
                return containerRepository.findByRiskLevelAndAnomalyFlagTrue(riskLevel, pageable);
            } else if (riskLevel != null && !riskLevel.isEmpty()) {
                return containerRepository.findByRiskLevel(riskLevel, pageable);
            } else if (Boolean.TRUE.equals(anomaly)) {
                return containerRepository.findByAnomalyFlagTrue(pageable);
            }
            return containerRepository.findAll(pageable);
        }
    }

    public void clearAllData(User user) {
        containerRepository.deleteAll();
        auditService.log(user, "CLEAR_ALL_DATA");
    }
}
