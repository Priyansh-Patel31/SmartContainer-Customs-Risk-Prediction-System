package com.smartcontainer.service;

import com.smartcontainer.entity.Container;
import com.smartcontainer.repository.ContainerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.opencsv.CSVWriter;

/**
 * ReportService — generates CSV exports matching the Node.js implementation.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ContainerRepository containerRepository;

    public byte[] generateSummaryCsv() throws Exception {
        List<Container> containers = containerRepository.findAll();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(239); // EF
        baos.write(187); // BB
        baos.write(191); // BF (UTF-8 BOM)

        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            writer.writeNext(new String[]{
                    "Container ID", "Declaration Date", "Origin", "Destination", 
                    "Importer ID", "Exporter ID", "Declared Weight", "Measured Weight",
                    "Declared Value", "HS Code", "Risk Level", "Risk Score", "Anomaly"
            });

            DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE;

            for (Container c : containers) {
                writer.writeNext(new String[]{
                        c.getContainerId(),
                        c.getDeclarationDate() != null ? c.getDeclarationDate().format(dtf) : "",
                        c.getOriginCountry(),
                        c.getDestinationCountry(),
                        c.getImporterId(),
                        c.getExporterId(),
                        c.getDeclaredWeight() != null ? String.valueOf(c.getDeclaredWeight()) : "",
                        c.getMeasuredWeight() != null ? String.valueOf(c.getMeasuredWeight()) : "",
                        c.getDeclaredValue() != null ? String.valueOf(c.getDeclaredValue()) : "",
                        c.getHsCode(),
                        c.getRiskLevel(),
                        c.getRiskScore() != null ? String.valueOf(c.getRiskScore()) : "",
                        Boolean.TRUE.equals(c.getAnomalyFlag()) ? "Yes" : "No"
                });
            }
        }
        return baos.toByteArray();
    }

    public byte[] generatePredictionsCsv(String batchId, String riskLevel) throws Exception {
        List<Container> containers;
        if (batchId != null) {
            containers = containerRepository.findByUploadBatchId(batchId);
            if (riskLevel != null) {
                containers = containers.stream().filter(c -> riskLevel.equals(c.getRiskLevel())).toList();
            }
        } else if (riskLevel != null) {
            containers = containerRepository.findByRiskLevel(riskLevel, org.springframework.data.domain.PageRequest.of(0, 10000)).getContent();
        } else {
            containers = containerRepository.findAll();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(239); // EF
        baos.write(187); // BB
        baos.write(191); // BF (UTF-8 BOM)

        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            writer.writeNext(new String[]{"Container_ID", "Risk_Score", "Risk_Level", "Explanation_Summary"});

            for (Container c : containers) {
                writer.writeNext(new String[]{
                        c.getContainerId(),
                        c.getRiskScore() != null ? String.format("%.4f", c.getRiskScore()) : "",
                        c.getRiskLevel() != null ? c.getRiskLevel() : "Clear",
                        c.getExplanation() != null ? c.getExplanation() : "No explanation available."
                });
            }
        }
        return baos.toByteArray();
    }

    public byte[] generatePdf() {
        // PDF generation using iText or PDFBox could be implemented here.
        // For brevity, returning empty byte array matching Node's stub/simulated PDF.
        return new byte[0];
    }
}
