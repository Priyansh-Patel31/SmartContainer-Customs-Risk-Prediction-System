package com.smartcontainer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * ShipmentTrack entity — tracks ship positions, stops, and timeline events.
 */
@Entity
@Table(name = "shipment_tracks", indexes = {
        @Index(name = "idx_track_container", columnList = "containerId", unique = true),
        @Index(name = "idx_track_vessel", columnList = "vesselImo"),
        @Index(name = "idx_track_status", columnList = "status"),
        @Index(name = "idx_track_status_risk", columnList = "status, riskLevel"),
        @Index(name = "idx_track_updated", columnList = "lastUpdated")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String containerId;

    private String vesselImo;
    private String vesselName;

    // Endpoints
    private String originName;
    private Double originLat;
    private Double originLng;
    private String destinationName;
    private Double destinationLat;
    private Double destinationLng;

    // Last position
    private Double lastPositionLat;
    private Double lastPositionLng;
    private LocalDateTime lastPositionTimestamp;
    private Double speedKnots;
    private Double heading;

    // Arrays stored as JSON text
    @Column(columnDefinition = "TEXT")
    private String stopsJson;

    @Column(columnDefinition = "TEXT")
    private String eventsJson;

    @Column(columnDefinition = "TEXT")
    private String routeGeojson;

    // ETA and voyage info
    private LocalDateTime eta;
    private LocalDateTime voyageStart;
    private Double estimatedDurationHours;
    private LocalDateTime actualDeparture;

    @Builder.Default
    private String status = "UNKNOWN";  // AT_SEA, IN_PORT, DELAYED, ARRIVED, UNKNOWN

    @Builder.Default
    private String provider = "SIMULATED";

    // Risk data (mirrored from Container)
    private String riskLevel;
    private Double riskScore;
    private Boolean anomalyFlag;

    @Builder.Default
    private Double progress = 0.0;

    @Builder.Default
    private LocalDateTime lastUpdated = LocalDateTime.now();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt2;
}
