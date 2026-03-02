package tn.esprit.abonnement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Stripe Checkout Session creation response
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckoutResponse {
    private String sessionId;
    private String sessionUrl;
}
