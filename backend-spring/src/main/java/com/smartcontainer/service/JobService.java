package com.smartcontainer.service;

import com.smartcontainer.entity.Job;
import com.smartcontainer.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * JobService — tracks background task status and logs.
 */
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;

    public Job createJob(String type, Long userId, String filename, String originalFilename) {
        String jobId = "job-" + java.util.UUID.randomUUID().toString();
        Job job = Job.builder()
                .jobId(jobId)
                .type(type)
                .createdBy(userId)
                .filename(filename)
                .originalFilename(originalFilename)
                .status("active")
                .progress(0)
                .logsJson("[]")
                .build();
        return jobRepository.save(job);
    }

    public Optional<Job> getJob(String jobId) {
        return jobRepository.findByJobId(jobId);
    }

    public void updateJobProgress(String jobId, int progress) {
        jobRepository.findByJobId(jobId).ifPresent(job -> {
            job.setProgress(progress);
            jobRepository.save(job);
        });
    }

    public void completeJob(String jobId, int total, int processed, int failed, String resultFile, String batchId) {
        jobRepository.findByJobId(jobId).ifPresent(job -> {
            job.setStatus("completed");
            job.setProgress(100);
            job.setTotalRecords(total);
            job.setProcessedRecords(processed);
            job.setFailedRecords(failed);
            job.setResultFile(resultFile);
            job.setBatchId(batchId);
            job.setCompletedAt(java.time.LocalDateTime.now());
            jobRepository.save(job);
        });
    }

    public void failJob(String jobId, String error) {
        jobRepository.findByJobId(jobId).ifPresent(job -> {
            job.setStatus("failed");
            job.setError(error);
            job.setCompletedAt(java.time.LocalDateTime.now());
            jobRepository.save(job);
        });
    }

    public Page<Job> listJobs(String status, String type, int page, int limit) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        if (status != null && type != null) {
            return jobRepository.findByStatusAndType(status, type, pageable);
        } else if (status != null) {
            return jobRepository.findByStatus(status, pageable);
        } else if (type != null) {
            return jobRepository.findByType(type, pageable);
        }
        return jobRepository.findAll(pageable);
    }

    public void deleteJob(String jobId) {
        jobRepository.deleteByJobId(jobId);
    }
}
