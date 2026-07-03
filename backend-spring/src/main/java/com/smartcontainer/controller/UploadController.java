package com.smartcontainer.controller;

import com.smartcontainer.dto.ApiResponse;
import com.smartcontainer.entity.Job;
import com.smartcontainer.entity.User;
import com.smartcontainer.service.JobService;
import com.smartcontainer.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Upload Controller — Maps to routes in uploadRoutes.js
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;
    private final JobService jobService;

    @Value("${app.upload-dir:./data/uploads}")
    private String uploadDir;

    @PostMapping({"", "/", "/stream"})
    public ResponseEntity<ApiResponse<Job>> uploadDataset(@RequestParam("file") MultipartFile file,
                                                          @RequestAttribute("currentUser") User currentUser) {
        
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Please upload a CSV file"));
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "dataset.csv");
        if (!originalFilename.endsWith(".csv")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Only CSV files are allowed"));
        }

        try {
            // Save file
            String newFilename = UUID.randomUUID().toString() + ".csv";
            File dest = new File(uploadDir, newFilename);
            file.transferTo(dest);

            // Create job
            Job job = jobService.createJob("UPLOAD_DATASET", currentUser.getId(), newFilename, originalFilename);

            // Start async processing
            uploadService.processUploadAsync(dest.getAbsolutePath(), job.getJobId(), originalFilename, currentUser);

            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.ok(job, "File uploaded successfully. Processing started in background."));
            
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to save file: " + e.getMessage()));
        }
    }
}
