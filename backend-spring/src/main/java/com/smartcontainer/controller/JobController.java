package com.smartcontainer.controller;

import com.smartcontainer.dto.ApiResponse;
import com.smartcontainer.entity.Job;
import com.smartcontainer.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.smartcontainer.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Job Controller — Maps to routes in jobRoutes.js
 */
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> listJobs(@RequestParam(required = false) String status,
                                                        @RequestParam(required = false) String type,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "20") int limit) {
        Page<Job> pagedResult = jobService.listJobs(status, type, page, limit);
        Map<String, Object> response = Map.of(
                "success", true,
                "data", pagedResult.getContent(),
                "total", pagedResult.getTotalElements(),
                "page", page,
                "limit", limit
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResponse<Job>> getJob(@PathVariable String jobId) {
        Job job = jobService.getJob(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        return ResponseEntity.ok(ApiResponse.ok(job));
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable String jobId) {
        jobService.deleteJob(jobId);
        return ResponseEntity.ok(ApiResponse.ok("Job deleted successfully"));
    }
}
