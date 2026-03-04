package tn.esprit.abonnement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InvoiceDTO {
    private Long id;
    private String invoiceNumber;
    private String planName;
    private Double amount;
    private String issuedAt;
    private String renewalDate;
    private String subscriptionStatus;
    private boolean paid;
    private String stripeSessionId;
}
