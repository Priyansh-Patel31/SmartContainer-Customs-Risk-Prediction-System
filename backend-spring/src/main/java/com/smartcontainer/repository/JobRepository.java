package com.smartcontainer.repository;

import com.smartcontainer.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    Optional<Job> findByJobId(String jobId);
    Page<Job> findByStatus(String status, Pageable pageable);
    Page<Job> findByType(String type, Pageable pageable);
    Page<Job> findByStatusAndType(String status, String type, Pageable pageable);
    void deleteByJobId(String jobId);
}
