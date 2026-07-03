package com.smartcontainer.repository;

import com.smartcontainer.entity.GeoCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GeoCacheRepository extends JpaRepository<GeoCache, Long> {
    Optional<GeoCache> findByCacheKey(String cacheKey);
}
