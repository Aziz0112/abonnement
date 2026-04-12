package tn.esprit.abonnement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HealthScoreDTO {
    private Integer healthScore;
    private String grade;
    private String status;
    private String color;
    private List<String> alerts;
    private Map<String, Double> breakdown;
}
