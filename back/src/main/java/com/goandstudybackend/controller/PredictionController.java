package com.goandstudybackend.controller;

import com.goandstudybackend.dto.response.ChurnPredictionResponse;
import com.goandstudybackend.service.PredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionService predictionService;

    @GetMapping("/churn/{userId}")
    public ResponseEntity<ChurnPredictionResponse> getChurnPrediction(@PathVariable String userId) {
        ChurnPredictionResponse response = predictionService.getChurnPrediction(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/churn")
    public ResponseEntity<List<ChurnPredictionResponse>> getAllChurnPredictions() {
        List<ChurnPredictionResponse> predictions = predictionService.getAllChurnPredictions();
        return ResponseEntity.ok(predictions);
    }
}
