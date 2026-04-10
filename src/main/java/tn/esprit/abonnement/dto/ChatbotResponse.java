package tn.esprit.abonnement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotResponse {
    private String response;
    private List<String> suggestedActions;
    private String recommendedPlan;
    private String intent;
}