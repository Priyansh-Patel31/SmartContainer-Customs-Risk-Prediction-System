package com.smartcontainer.service;

import com.smartcontainer.entity.GeoCache;
import com.smartcontainer.repository.GeoCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * GeoService — handles basic geocoding and caching.
 * Real geocoding APIs would go here, we're using a simplified static mapping for the prototype.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeoService {

    private final GeoCacheRepository geoCacheRepository;

    // Static fallback coordinates for common countries to match Node.js behavior
    private static final Map<String, double[]> STATIC_COORDS = new HashMap<>();
    static {
        STATIC_COORDS.put("China", new double[]{35.8617, 104.1954});
        STATIC_COORDS.put("United States", new double[]{37.0902, -95.7129});
        STATIC_COORDS.put("Germany", new double[]{51.1657, 10.4515});
        STATIC_COORDS.put("Japan", new double[]{36.2048, 138.2529});
        STATIC_COORDS.put("India", new double[]{20.5937, 78.9629});
        STATIC_COORDS.put("Brazil", new double[]{-14.2350, -51.9253});
        STATIC_COORDS.put("UK", new double[]{55.3781, -3.4360});
        STATIC_COORDS.put("United Kingdom", new double[]{55.3781, -3.4360});
        STATIC_COORDS.put("Australia", new double[]{-25.2744, 133.7751});
        STATIC_COORDS.put("South Africa", new double[]{-30.5595, 22.9375});
        STATIC_COORDS.put("Singapore", new double[]{1.3521, 103.8198});
        STATIC_COORDS.put("UAE", new double[]{23.4241, 53.8478});
        STATIC_COORDS.put("Netherlands", new double[]{52.1326, 5.2913});
    }

    public double[] geocode(String location) {
        if (location == null || location.isEmpty()) return null;
        
        String cacheKey = location.trim().toLowerCase();
        
        // 1. Check DB Cache
        GeoCache cached = geoCacheRepository.findByCacheKey(cacheKey).orElse(null);
        if (cached != null) {
            cached.setHitCount(cached.getHitCount() + 1);
            geoCacheRepository.save(cached);
            return new double[]{cached.getLat(), cached.getLng()};
        }

        // 2. Check Static Map
        // Trying exact match first
        double[] coords = STATIC_COORDS.get(location);
        if (coords == null) {
            // Try matching any part of the string
            for (Map.Entry<String, double[]> entry : STATIC_COORDS.entrySet()) {
                if (location.toLowerCase().contains(entry.getKey().toLowerCase())) {
                    coords = entry.getValue();
                    break;
                }
            }
        }

        // 3. Save to DB cache if found
        if (coords != null) {
            GeoCache newCache = GeoCache.builder()
                    .cacheKey(cacheKey)
                    .location(location)
                    .lat(coords[0])
                    .lng(coords[1])
                    .source("static")
                    .build();
            geoCacheRepository.save(newCache);
        }

        return coords;
    }
}
