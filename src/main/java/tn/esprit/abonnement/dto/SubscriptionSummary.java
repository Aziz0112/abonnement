package tn.esprit.abonnement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionSummary {
    private Long userId;
    private String currentPlan;
    private String status;
    private LocalDateTime expiresAt;
    private boolean autoRenew;
    private Double monthlyPrice;
}