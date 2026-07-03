package com.smartcontainer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcontainer.entity.Container;
import com.smartcontainer.entity.ShipmentTrack;
import com.smartcontainer.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.smartcontainer.repository.ContainerRepository;
import com.smartcontainer.repository.ShipmentTrackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TrackingService — tracks ship positions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingService {

    private final ShipmentTrackRepository trackRepository;
    private final ContainerRepository containerRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> getTrack(String containerId) {
        ShipmentTrack track = trackRepository.findByContainerId(containerId)
                .orElseGet(() -> createMockTrack(containerId));

        Map<String, Object> response = new HashMap<>();
        response.put("container_id", track.getContainerId());
        response.put("vessel_imo", track.getVesselImo());
        response.put("vessel_name", track.getVesselName());
        response.put("status", track.getStatus());
        response.put("risk_level", track.getRiskLevel());
        response.put("risk_score", track.getRiskScore());
        
        Map<String, Object> currentPosition = new HashMap<>();
        currentPosition.put("lat", track.getLastPositionLat());
        currentPosition.put("lng", track.getLastPositionLng());
        currentPosition.put("timestamp", track.getLastPositionTimestamp());
        currentPosition.put("speed_knots", track.getSpeedKnots());
        currentPosition.put("heading", track.getHeading());
        response.put("current_position", currentPosition);

        try {
            if (track.getStopsJson() != null) {
                response.put("stops", objectMapper.readValue(track.getStopsJson(), new TypeReference<List<Map<String, Object>>>() {}));
            }
            if (track.getEventsJson() != null) {
                response.put("events", objectMapper.readValue(track.getEventsJson(), new TypeReference<List<Map<String, Object>>>() {}));
            }
            if (track.getRouteGeojson() != null) {
                response.put("route_geojson", objectMapper.readValue(track.getRouteGeojson(), Map.class));
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse tracking JSON arrays for {}", containerId, e);
        }

        return response;
    }

    public Map<String, Object> getTracks(String riskLevel, String status, int limit) {
        List<ShipmentTrack> tracks;
        PageRequest page = PageRequest.of(0, limit);

        if (riskLevel != null && status != null) {
            tracks = trackRepository.findByStatusAndRiskLevel(status, riskLevel, page);
        } else if (riskLevel != null) {
            tracks = trackRepository.findByRiskLevel(riskLevel, page);
        } else if (status != null) {
            tracks = trackRepository.findByStatus(status, page);
        } else {
            tracks = trackRepository.findAll(page).getContent();
        }

        List<Map<String, Object>> features = new ArrayList<>();
        
        for (ShipmentTrack track : tracks) {
            if (track.getLastPositionLat() != null && track.getLastPositionLng() != null) {
                Map<String, Object> feature = new HashMap<>();
                feature.put("type", "Feature");
                
                Map<String, Object> properties = new HashMap<>();
                properties.put("container_id", track.getContainerId());
                properties.put("vessel_name", track.getVesselName());
                properties.put("status", track.getStatus());
                properties.put("risk_level", track.getRiskLevel());
                feature.put("properties", properties);

                Map<String, Object> geometry = new HashMap<>();
                geometry.put("type", "Point");
                geometry.put("coordinates", new double[]{track.getLastPositionLng(), track.getLastPositionLat()});
                feature.put("geometry", geometry);
                
                features.add(feature);
            }
        }

        Map<String, Object> geoJson = new HashMap<>();
        geoJson.put("type", "FeatureCollection");
        geoJson.put("features", features);
        return geoJson;
    }

    public void linkVessel(String containerId, String vesselImo, String vesselName) {
        ShipmentTrack track = trackRepository.findByContainerId(containerId)
                .orElseGet(() -> createMockTrack(containerId));
        track.setVesselImo(vesselImo);
        if (vesselName != null) {
            track.setVesselName(vesselName);
        }
        trackRepository.save(track);
    }

    public void forceRefresh(String containerId) {
        ShipmentTrack track = trackRepository.findByContainerId(containerId)
                .orElseThrow(() -> new ResourceNotFoundException("Track not found for container: " + containerId));
        
        // Slightly alter the position to simulate movement
        if (track.getLastPositionLat() != null) {
            track.setLastPositionLat(track.getLastPositionLat() + (Math.random() * 0.02 - 0.01));
            track.setLastPositionLng(track.getLastPositionLng() + (Math.random() * 0.02 - 0.01));
            track.setLastPositionTimestamp(LocalDateTime.now());
            trackRepository.save(track);
        }
    }

    private ShipmentTrack createMockTrack(String containerId) {
        Container c = containerRepository.findByContainerId(containerId).orElse(null);
        
        ShipmentTrack track = new ShipmentTrack();
        track.setContainerId(containerId);
        track.setVesselName("MSC " + containerId.substring(0, Math.min(4, containerId.length())));
        track.setStatus("AT_SEA");
        track.setProvider("SIMULATED");
        
        if (c != null) {
            track.setRiskLevel(c.getRiskLevel());
            track.setRiskScore(c.getRiskScore());
            track.setAnomalyFlag(c.getAnomalyFlag());
            
            // Generate some random coordinates between origin and destination if available
            if (c.getOriginLat() != null && c.getDestinationLat() != null) {
                double fraction = Math.random();
                track.setLastPositionLat(c.getOriginLat() + (c.getDestinationLat() - c.getOriginLat()) * fraction);
                track.setLastPositionLng(c.getOriginLng() + (c.getDestinationLng() - c.getOriginLng()) * fraction);
            } else {
                // Fallback to random ocean coordinates (Mid Atlantic)
                track.setLastPositionLat(25.0 + Math.random() * 10);
                track.setLastPositionLng(-40.0 + Math.random() * 10);
            }
        } else {
            track.setLastPositionLat(25.0);
            track.setLastPositionLng(-40.0);
        }

        track.setLastPositionTimestamp(LocalDateTime.now());
        track.setSpeedKnots(15.0 + Math.random() * 10);
        track.setHeading(Math.random() * 360);
        
        track.setStopsJson("[]");
        track.setEventsJson("[]");
        
        return trackRepository.save(track);
    }
}
