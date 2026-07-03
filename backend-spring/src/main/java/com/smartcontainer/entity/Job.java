package com.smartcontainer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Job entity — tracks background processing tasks (upload, batch-predict, retrain).
 */
@Entity
@Table(name = "jobs", indexes = {
        @Index(name = "idx_job_jobid", columnList = "jobId", unique = true),
        @Index(name = "idx_job_type", columnList = "type"),
        @Index(name = "idx_job_status", columnList = "status"),
        @Index(name = "idx_job_created", columnList = "createdAt, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String jobId;

    @Column(nullable = false)
    private String type;  // UPLOAD_DATASET, BATCH_PREDICT, RETRAIN_MODEL

    @Builder.Default
    private String status = "waiting";  // waiting, active, completed, failed

    @Builder.Default
    private Integer progress = 0;

    private Long createdBy;

    // Job-specific metadata
    private String filename;
    private String originalFilename;
    private Integer totalRecords;
    private Integer processedRecords;
    private Integer failedRecords;
    private String batchId;
    private String resultFile;
    private String filePath;

    @Column(columnDefinition = "TEXT")
    private String logsJson;  // JSON array of log entries

    @Column(columnDefinition = "TEXT")
    private String error;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
