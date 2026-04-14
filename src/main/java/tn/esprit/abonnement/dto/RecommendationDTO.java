package tn.esprit.abonnement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.abonnement.entity.PlanType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationDTO {
    private PlanType recommendedPlan;
    private String reason;
    private String state;
}
