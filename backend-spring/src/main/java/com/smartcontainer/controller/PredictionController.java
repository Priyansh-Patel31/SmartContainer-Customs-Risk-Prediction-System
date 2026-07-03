package com.smartcontainer.controller;

import com.smartcontainer.dto.ApiResponse;
import com.smartcontainer.entity.Container;
import com.smartcontainer.service.PredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Prediction Controller — Maps to routes in predictionRoutes.js
 */
@RestController
@RequestMapping("/api/predict")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionService predictionService;

    @PostMapping({"", "/"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> predictSingle(@RequestBody Container containerRequest) {
        Map<String, Object> result = predictionService.predictSingle(containerRequest);
        return ResponseEntity.ok(ApiResponse.ok(result, "Risk assessment complete"));
    }

    @PostMapping("/train")
    public ResponseEntity<ApiResponse<Map<String, Object>>> retrainModel() {
        Map<String, Object> result = predictionService.triggerTraining();
        return ResponseEntity.ok(ApiResponse.ok(result, "Model retraining triggered"));
    }
}
