package com.smartcontainer.service;

import com.smartcontainer.entity.Container;
import com.smartcontainer.repository.ContainerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MapService — handles geospatial analytics and heatmap generation.
 */
@Service
@RequiredArgsConstructor
public class MapService {

    private final ContainerRepository containerRepository;
    private final GeoService geoService;

    public Map<String, Object> getAllRoutes() {
        List<Container> containers = containerRepository.findByOriginLatIsNotNullAndDestinationLatIsNotNull(PageRequest.of(0, 500));
        
        List<Map<String, Object>> features = new ArrayList<>();
        
        for (Container c : containers) {
            Map<String, Object> feature = new HashMap<>();
            feature.put("type", "Feature");
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("container_id", c.getContainerId());
            properties.put("risk_level", c.getRiskLevel());
            properties.put("origin", c.getOriginCountry());
            properties.put("destination", c.getDestinationCountry());
            properties.put("risk_score", c.getRiskScore());
            feature.put("properties", properties);

            Map<String, Object> geometry = new HashMap<>();
            geometry.put("type", "LineString");
            geometry.put("coordinates", new double[][]{
                {c.getOriginLng(), c.getOriginLat()},
                {c.getDestinationLng(), c.getDestinationLat()}
            });
            feature.put("geometry", geometry);
            
            features.add(feature);
        }

        Map<String, Object> geoJson = new HashMap<>();
        geoJson.put("type", "FeatureCollection");
        geoJson.put("features", features);
        return geoJson;
    }

    public List<Map<String, Object>> getHeatmapData() {
        List<Container> containers = containerRepository.findByRiskScoreIsNotNull();
        List<Map<String, Object>> heatmap = new ArrayList<>();

        for (Container c : containers) {
            if (c.getOriginLat() != null && c.getOriginLng() != null) {
                Map<String, Object> point = new HashMap<>();
                point.put("lat", c.getOriginLat());
                point.put("lng", c.getOriginLng());
                point.put("intensity", c.getRiskScore());
                heatmap.add(point);
            }
        }
        return heatmap;
    }
}
