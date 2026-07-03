package com.smartcontainer.service;

import com.opencsv.CSVReader;
import com.smartcontainer.entity.Container;
import com.smartcontainer.entity.Job;
import com.smartcontainer.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * UploadService — Handles background processing of uploaded CSV datasets.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UploadService {

    private final JobService jobService;
    private final PredictionService predictionService;
    private final AuditService auditService;

    @Async
    public CompletableFuture<Void> processUploadAsync(String filePath, String jobId, String originalFilename, User user) {
        try {
            jobService.updateJobProgress(jobId, 5);
            List<Container> records = parseCsv(filePath);
            jobService.updateJobProgress(jobId, 25);

            String batchId = "batch-" + UUID.randomUUID().toString();
            
            // Process the batch via prediction service
            Map<String, Object> result = predictionService.predictBatch(records, batchId);
            
            List<?> predictions = (List<?>) result.get("results");
            int processed = predictions.size();
            
            jobService.completeJob(jobId, records.size(), processed, records.size() - processed, null, batchId);
            auditService.log(user, "UPLOAD_DATASET", "Job", jobId);

        } catch (Exception e) {
            log.error("Failed to process upload job {}", jobId, e);
            jobService.failJob(jobId, e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    private List<Container> parseCsv(String filePath) throws Exception {
        List<Container> records = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headers = reader.readNext();
            if (headers == null) return records;

            String[] line;
            while ((line = reader.readNext()) != null) {
                if (line.length == 0 || line[0].trim().isEmpty()) continue;
                
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < headers.length && i < line.length; i++) {
                    String header = headers[i].trim().toLowerCase().replaceAll("[\\s-]", "_");
                    row.put(header, line[i]);
                }
                
                Container c = new Container();
                c.setContainerId(row.get("container_id"));
                
                if (row.containsKey("declared_weight")) c.setDeclaredWeight(parseDouble(row.get("declared_weight")));
                if (row.containsKey("measured_weight")) c.setMeasuredWeight(parseDouble(row.get("measured_weight")));
                if (row.containsKey("declared_value")) c.setDeclaredValue(parseDouble(row.get("declared_value")));
                if (row.containsKey("dwell_time_hours")) c.setDwellTimeHours(parseDouble(row.get("dwell_time_hours")));
                
                c.setTradeRegime(row.get("trade_regime"));
                c.setOriginCountry(row.get("origin_country"));
                c.setDestinationCountry(row.get("destination_country"));
                c.setDestinationPort(row.get("destination_port"));
                c.setHsCode(row.get("hs_code"));
                c.setImporterId(row.get("importer_id"));
                c.setExporterId(row.get("exporter_id"));
                c.setShippingLine(row.get("shipping_line"));
                
                records.add(c);
            }
        }
        return records;
    }

    private Double parseDouble(String val) {
        if (val == null || val.trim().isEmpty()) return null;
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
