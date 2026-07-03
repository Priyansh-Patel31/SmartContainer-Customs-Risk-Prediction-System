package com.smartcontainer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * GeoCache entity — persists geocoding results to reduce API calls.
 */
@Entity
@Table(name = "geo_cache", indexes = {
        @Index(name = "idx_geocache_key", columnList = "cacheKey", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeoCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cache_key", nullable = false, unique = true)
    private String cacheKey;

    private String location;
    private Double lat;
    private Double lng;

    @Builder.Default
    private String source = "api";  // static, api, manual

    @Builder.Default
    private Integer hitCount = 0;

    private LocalDateTime expiresAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
