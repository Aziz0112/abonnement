package tn.esprit.abonnement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChurnBatchResponseDTO {
    private List<ChurnPredictionDTO> predictions;
    private Map<String, Double> featureImportance;
}
