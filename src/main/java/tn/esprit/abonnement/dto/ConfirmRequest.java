package tn.esprit.abonnement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for confirming a Stripe payment and activating the subscription
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmRequest {

    @NotBlank(message = "Session ID is required")
    private String sessionId;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Plan ID is required")
    @Positive(message = "Plan ID must be positive")
    private Long planId;

    private String email;
}
