package tn.esprit.abonnement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for sending a dynamic email from the frontend
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailRequest {

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email address")
    private String toEmail;

    @NotBlank(message = "Subject is required")
    private String subject;

    private String userName;

    private String planName;

    private String amount;

    private String subscriptionDate;

    private String expirationDate;
}
