package com.smartcontainer.repository;

import com.smartcontainer.entity.ShipmentTrack;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentTrackRepository extends JpaRepository<ShipmentTrack, Long> {
    Optional<ShipmentTrack> findByContainerId(String containerId);
    List<ShipmentTrack> findByStatus(String status, Pageable pageable);
    List<ShipmentTrack> findByRiskLevel(String riskLevel, Pageable pageable);
    List<ShipmentTrack> findByStatusAndRiskLevel(String status, String riskLevel, Pageable pageable);
}
