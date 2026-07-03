package com.smartcontainer.repository;

import com.smartcontainer.entity.Container;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContainerRepository extends JpaRepository<Container, Long> {

    Optional<Container> findByContainerId(String containerId);

    boolean existsByContainerId(String containerId);

    // Dashboard summary counts
    long countByRiskLevel(String riskLevel);
    long countByAnomalyFlagTrue();
    long countByRiskLevelIsNull();

    // Exporter-filtered counts
    long countByExporterId(String exporterId);
    long countByExporterIdAndRiskLevel(String exporterId, String riskLevel);
    long countByExporterIdAndAnomalyFlagTrue(String exporterId);
    long countByExporterIdAndRiskLevelIsNull(String exporterId);

    // Risk distribution
    @Query("SELECT c.riskLevel, COUNT(c) FROM Container c WHERE c.riskLevel IS NOT NULL GROUP BY c.riskLevel ORDER BY COUNT(c) DESC")
    List<Object[]> getRiskDistribution();

    @Query("SELECT c.riskLevel, COUNT(c) FROM Container c WHERE c.riskLevel IS NOT NULL AND c.exporterId = :exporterId GROUP BY c.riskLevel ORDER BY COUNT(c) DESC")
    List<Object[]> getRiskDistributionByExporter(@Param("exporterId") String exporterId);

    // Top risky routes
    @Query("SELECT c.originCountry, c.destinationCountry, COUNT(c), AVG(c.riskScore), SUM(CASE WHEN c.anomalyFlag = true THEN 1 ELSE 0 END) " +
           "FROM Container c WHERE c.riskLevel = 'Critical' GROUP BY c.originCountry, c.destinationCountry ORDER BY COUNT(c) DESC")
    List<Object[]> getTopRiskyRoutes(Pageable pageable);

    // Anomaly by origin country
    @Query("SELECT c.originCountry, COUNT(c) FROM Container c WHERE c.anomalyFlag = true GROUP BY c.originCountry ORDER BY COUNT(c) DESC")
    List<Object[]> getAnomalyByCountry(Pageable pageable);

    // Anomaly by risk level
    @Query("SELECT c.riskLevel, COUNT(c) FROM Container c WHERE c.anomalyFlag = true GROUP BY c.riskLevel")
    List<Object[]> getAnomalyByRiskLevel();

    // Recent high risk
    List<Container> findByRiskLevelOrderByProcessedAtDesc(String riskLevel, Pageable pageable);

    List<Container> findByRiskLevelAndExporterIdOrderByProcessedAtDesc(String riskLevel, String exporterId, Pageable pageable);

    // Paginated containers with optional filters
    Page<Container> findByRiskLevel(String riskLevel, Pageable pageable);
    Page<Container> findByAnomalyFlagTrue(Pageable pageable);
    Page<Container> findByRiskLevelAndAnomalyFlagTrue(String riskLevel, Pageable pageable);
    Page<Container> findByExporterId(String exporterId, Pageable pageable);
    Page<Container> findByExporterIdAndRiskLevel(String exporterId, String riskLevel, Pageable pageable);

    // Route analytics
    @Query("SELECT c.originCountry, c.destinationCountry, COUNT(c), " +
           "SUM(CASE WHEN c.riskLevel = 'Critical' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN c.riskLevel = 'Low Risk' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN c.riskLevel = 'Clear' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN c.anomalyFlag = true THEN 1 ELSE 0 END), " +
           "AVG(c.riskScore), AVG(c.dwellTimeHours) " +
           "FROM Container c WHERE c.originCountry IS NOT NULL AND c.destinationCountry IS NOT NULL " +
           "GROUP BY c.originCountry, c.destinationCountry ORDER BY " +
           "CAST(SUM(CASE WHEN c.riskLevel = 'Critical' THEN 1 ELSE 0 END) AS double) / CAST(COUNT(c) AS double) DESC")
    List<Object[]> getRouteRiskAnalytics(Pageable pageable);

    // Suspicious importers
    @Query("SELECT c.importerId, COUNT(c), " +
           "SUM(CASE WHEN c.riskLevel = 'Critical' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN c.anomalyFlag = true THEN 1 ELSE 0 END), " +
           "AVG(c.riskScore) " +
           "FROM Container c WHERE c.importerId IS NOT NULL " +
           "GROUP BY c.importerId HAVING SUM(CASE WHEN c.riskLevel = 'Critical' THEN 1 ELSE 0 END) > 0 " +
           "ORDER BY SUM(CASE WHEN c.riskLevel = 'Critical' THEN 1 ELSE 0 END) DESC")
    List<Object[]> getSuspiciousImporters(Pageable pageable);

    // Risk trend — count by day and risk level
    @Query("SELECT CAST(c.processedAt AS date), c.riskLevel, COUNT(c) " +
           "FROM Container c WHERE c.processedAt >= :since AND c.riskLevel IS NOT NULL " +
           "GROUP BY CAST(c.processedAt AS date), c.riskLevel ORDER BY CAST(c.processedAt AS date)")
    List<Object[]> getRiskTrend(@Param("since") LocalDateTime since);

    // Importer stats
    long countByImporterId(String importerId);
    long countByImporterIdAndRiskLevel(String importerId, String riskLevel);

    // Map queries
    List<Container> findByOriginLatIsNotNullAndDestinationLatIsNotNull(Pageable pageable);
    long countByOriginLatIsNotNullAndDestinationLatIsNotNull();

    // Heatmap — all containers with risk score
    List<Container> findByRiskScoreIsNotNull();

    // Queue — inspection queue sorted by risk
    @Query("SELECT c FROM Container c WHERE c.inspectionStatus IN ('NEW', 'ASSIGNED', 'IN_REVIEW') " +
           "ORDER BY c.riskScore DESC NULLS LAST")
    Page<Container> getInspectionQueue(Pageable pageable);

    // Upload batches
    @Query("SELECT c.uploadBatchId, COUNT(c), MIN(c.createdAt) " +
           "FROM Container c WHERE c.uploadBatchId IS NOT NULL " +
           "GROUP BY c.uploadBatchId ORDER BY MIN(c.createdAt) DESC")
    List<Object[]> getUploadBatches(Pageable pageable);

    // Batch containers for reporting
    List<Container> findByUploadBatchId(String batchId);

    // Fraud patterns — HS codes
    @Query("SELECT c.hsCode, COUNT(c), SUM(CASE WHEN c.riskLevel = 'Critical' THEN 1 ELSE 0 END), AVG(c.riskScore) " +
           "FROM Container c WHERE c.hsCode IS NOT NULL GROUP BY c.hsCode " +
           "HAVING COUNT(c) >= 3 AND SUM(CASE WHEN c.riskLevel = 'Critical' THEN 1 ELSE 0 END) > 0 " +
           "ORDER BY CAST(SUM(CASE WHEN c.riskLevel = 'Critical' THEN 1 ELSE 0 END) AS double) / CAST(COUNT(c) AS double) DESC")
    List<Object[]> getFraudHsCodes(Pageable pageable);

    // Fraud patterns — shipping lines
    @Query("SELECT c.shippingLine, COUNT(c), SUM(CASE WHEN c.riskLevel = 'Critical' THEN 1 ELSE 0 END), AVG(c.riskScore) " +
           "FROM Container c WHERE c.shippingLine IS NOT NULL GROUP BY c.shippingLine " +
           "HAVING COUNT(c) >= 5 AND SUM(CASE WHEN c.riskLevel = 'Critical' THEN 1 ELSE 0 END) > 0 " +
           "ORDER BY CAST(SUM(CASE WHEN c.riskLevel = 'Critical' THEN 1 ELSE 0 END) AS double) / CAST(COUNT(c) AS double) DESC")
    List<Object[]> getFraudShippingLines(Pageable pageable);

    // Importer risk history
    @Query("SELECT c.importerId, COUNT(c), SUM(CASE WHEN c.riskLevel = 'Critical' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN c.autoEscalatedByImporterHistory = true THEN 1 ELSE 0 END), " +
           "AVG(c.riskScore), MAX(c.processedAt) " +
           "FROM Container c WHERE c.importerId IS NOT NULL AND c.riskLevel IS NOT NULL " +
           "GROUP BY c.importerId ORDER BY " +
           "CAST(SUM(CASE WHEN c.riskLevel = 'Critical' THEN 1 ELSE 0 END) AS double) / CAST(COUNT(c) AS double) DESC")
    List<Object[]> getImporterRiskHistory(Pageable pageable);

    // Escalation stats
    long countByRiskLevelIsNotNull();
    long countByAutoEscalatedByImporterHistoryTrue();

    @Query("SELECT c.importerId, COUNT(c), AVG(c.importerCriticalPercentage) " +
           "FROM Container c WHERE c.autoEscalatedByImporterHistory = true " +
           "GROUP BY c.importerId ORDER BY COUNT(c) DESC")
    List<Object[]> getEscalationByImporter(Pageable pageable);

    // Unprocessed count
    long countByProcessedAtIsNull();

    // Notifications — recent status changes
    @Query("SELECT c FROM Container c WHERE c.inspectionStatus IN ('ASSIGNED', 'IN_REVIEW', 'HOLD', 'DETENTION') " +
           "ORDER BY c.updatedAt DESC")
    List<Container> getRecentInspectionActivity(Pageable pageable);
}
