package tn.esprit.abonnement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChurnPredictionDTO {
    private Long userId;
    private double churnProbability;
    private String riskLevel;
    private Map<String, Double> featureImportance;
}
