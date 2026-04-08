package tn.esprit.abonnement.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.esprit.abonnement.dto.ChurnBatchResponseDTO;
import tn.esprit.abonnement.dto.ChurnPredictionDTO;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChurnPredictorService {

    private final RestTemplate restTemplate;

    @Value("${churn.predictor.url}")
    private String churnPredictorUrl;

    /**
     * Predict churn for a single user by providing feature values.
     */
    public ChurnPredictionDTO predictSingle(Long userId, Map<String, Object> features) {
        String url = churnPredictorUrl + "/predict";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(features, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> body = response.getBody();

            ChurnPredictionDTO dto = new ChurnPredictionDTO();
            dto.setUserId(userId);
            dto.setChurnProbability(((Number) body.get("churn_probability")).doubleValue());
            dto.setRiskLevel((String) body.get("risk_level"));
            dto.setFeatureImportance((Map<String, Double>) body.get("feature_importance"));
            return dto;
        } catch (Exception e) {
            log.error("Failed to call churn predictor for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Churn prediction service unavailable: " + e.getMessage());
        }
    }

    /**
     * Predict churn for multiple users in a single batch call.
     */
    public ChurnBatchResponseDTO predictBatch(List<Map<String, Object>> usersPayload) {
        String url = churnPredictorUrl + "/predict-batch";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("users", usersPayload);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> responseBody = response.getBody();

            List<Map<String, Object>> rawPredictions = (List<Map<String, Object>>) responseBody.get("predictions");
            List<ChurnPredictionDTO> predictions = new ArrayList<>();

            for (Map<String, Object> raw : rawPredictions) {
                ChurnPredictionDTO dto = new ChurnPredictionDTO();
                dto.setUserId(raw.get("userId") != null ? ((Number) raw.get("userId")).longValue() : null);
                dto.setChurnProbability(((Number) raw.get("churn_probability")).doubleValue());
                dto.setRiskLevel((String) raw.get("risk_level"));
                predictions.add(dto);
            }

            Map<String, Double> featureImportance = (Map<String, Double>) responseBody.get("feature_importance");

            return new ChurnBatchResponseDTO(predictions, featureImportance);
        } catch (Exception e) {
            log.error("Failed to call churn predictor batch: {}", e.getMessage());
            throw new RuntimeException("Churn prediction service unavailable: " + e.getMessage());
        }
    }

    /**
     * Health check for the churn predictor Flask service.
     */
    public Map<String, Object> healthCheck() {
        String url = churnPredictorUrl + "/health";
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Churn predictor health check failed: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("status", "unavailable");
            result.put("error", e.getMessage());
            return result;
        }
    }
}
