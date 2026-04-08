package tn.esprit.abonnement.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.abonnement.dto.ChurnBatchResponseDTO;
import tn.esprit.abonnement.dto.ChurnPredictionDTO;
import tn.esprit.abonnement.services.ChurnPredictorService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/abonnements/churn")
@RequiredArgsConstructor
public class ChurnPredictorController {

    private final ChurnPredictorService churnPredictorService;

    /**
     * POST /api/abonnements/churn/predict
     * Body: { "userId": 1, "features": { "login_frequency": 2.5, ... } }
     */
    @PostMapping("/predict")
    public ResponseEntity<ChurnPredictionDTO> predict(@RequestBody Map<String, Object> body) {
        Long userId = body.get("userId") != null ? ((Number) body.get("userId")).longValue() : null;
        Map<String, Object> features = (Map<String, Object>) body.get("features");

        if (features == null || features.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ChurnPredictionDTO result = churnPredictorService.predictSingle(userId, features);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/abonnements/churn/predict-batch
     * Body: { "users": [ { "userId": 1, "features": { ... } }, ... ] }
     */
    @PostMapping("/predict-batch")
    public ResponseEntity<ChurnBatchResponseDTO> predictBatch(@RequestBody Map<String, Object> body) {
        List<Map<String, Object>> users = (List<Map<String, Object>>) body.get("users");

        if (users == null || users.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ChurnBatchResponseDTO result = churnPredictorService.predictBatch(users);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/abonnements/churn/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(churnPredictorService.healthCheck());
    }
}
