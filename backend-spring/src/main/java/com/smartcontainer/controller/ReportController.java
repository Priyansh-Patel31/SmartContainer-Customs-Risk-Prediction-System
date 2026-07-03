package com.smartcontainer.controller;

import com.smartcontainer.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Report Controller — Maps to routes in reportRoutes.js
 */
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/summary.csv")
    public ResponseEntity<byte[]> getSummaryCsv() throws Exception {
        byte[] csvData = reportService.generateSummaryCsv();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"full_report.csv\"");
        headers.setContentType(MediaType.parseMediaType("text/csv"));

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvData);
    }

    @GetMapping("/predictions.csv")
    public ResponseEntity<byte[]> getPredictionsCsv(@RequestParam(required = false) String batchId,
                                                    @RequestParam(required = false) String riskLevel) throws Exception {
        byte[] csvData = reportService.generatePredictionsCsv(batchId, riskLevel);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"predictions.csv\"");
        headers.setContentType(MediaType.parseMediaType("text/csv"));

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvData);
    }

    @GetMapping("/summary.pdf")
    public ResponseEntity<byte[]> getSummaryPdf() {
        byte[] pdfData = reportService.generatePdf();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"risk_summary_report.pdf\"");
        headers.setContentType(MediaType.APPLICATION_PDF);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfData);
    }
}
