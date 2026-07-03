package com.smartcontainer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Container entity — mirrors the MongoDB containerModel.js schema.
 * Stores raw shipment data, engineered features, ML outputs, and workflow state.
 */
@Entity
@Table(name = "containers", indexes = {
        @Index(name = "idx_container_id", columnList = "containerId", unique = true),
        @Index(name = "idx_risk_level_anomaly", columnList = "riskLevel, anomalyFlag"),
        @Index(name = "idx_final_risk_anomaly", columnList = "finalRiskLevel, anomalyFlag"),
        @Index(name = "idx_origin_dest", columnList = "originCountry, destinationCountry"),
        @Index(name = "idx_batch_risk", columnList = "uploadBatchId, riskLevel"),
        @Index(name = "idx_inspection_risk", columnList = "inspectionStatus, riskScore"),
        @Index(name = "idx_assigned_status", columnList = "assignedTo, inspectionStatus"),
        @Index(name = "idx_escalated_risk", columnList = "autoEscalatedByImporterHistory, finalRiskLevel"),
        @Index(name = "idx_importer", columnList = "importerId"),
        @Index(name = "idx_exporter", columnList = "exporterId"),
        @Index(name = "idx_upload_batch", columnList = "uploadBatchId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Container {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Core Identifiers ---
    @Column(nullable = false, unique = true)
    private String containerId;

    private LocalDate declarationDate;
    private String declarationTime;

    // --- Trade Information ---
    private String tradeRegime;
    private String originCountry;
    private String destinationCountry;
    private String destinationPort;
    private String hsCode;

    // --- Party Identifiers ---
    private String importerId;
    private String exporterId;

    // --- Financial & Physical ---
    private Double declaredValue;
    private Double declaredWeight;
    private Double measuredWeight;
    private String shippingLine;
    private Double dwellTimeHours;
    private String clearanceStatus;

    // --- Engineered Features ---
    private Double weightDifference;
    private Double weightMismatchPercentage;
    private Double valueToWeightRatio;
    private Boolean highDwellTimeFlag;
    private Integer importerFrequency;
    private Integer exporterFrequency;
    private Double tradeRouteRisk;

    // --- ML Outputs ---
    private Double riskScore;
    private String riskLevel;   // Critical, Low Risk, Clear
    private Boolean anomalyFlag;
    private Double anomalyScore;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    // --- Dual-Risk Tracking ---
    private Double modelRiskScore;
    private String modelRiskLevel;
    private Double finalRiskScore;
    private String finalRiskLevel;

    // --- Importer History Auto-Escalation ---
    @Builder.Default
    private Boolean autoEscalatedByImporterHistory = false;
    @Builder.Default
    private Double importerCriticalPercentage = 0.0;

    @Column(columnDefinition = "TEXT")
    private String overrideReason;

    @Column(columnDefinition = "TEXT")
    private String explanationSummary;

    private String predictionSource;  // 'single' or 'batch'

    // --- Geo Data ---
    private Double originLat;
    private Double originLng;
    private Double destinationLat;
    private Double destinationLng;

    @Column(columnDefinition = "TEXT")
    private String routePathJson;  // JSON array of [lat, lng] pairs

    // --- Metadata ---
    private String uploadBatchId;
    private LocalDateTime processedAt;

    // --- Customs Workflow ---
    @Builder.Default
    private String inspectionStatus = "NEW";  // NEW, ASSIGNED, IN_REVIEW, CLEARED, HOLD, DETENTION

    private String assignedTo;

    @Column(columnDefinition = "TEXT")
    private String notesJson;  // JSON array of note objects

    @ElementCollection
    @CollectionTable(name = "container_risk_explanations", joinColumns = @JoinColumn(name = "container_id"))
    @Column(name = "explanation_text")
    @Builder.Default
    private List<String> riskExplanation = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
