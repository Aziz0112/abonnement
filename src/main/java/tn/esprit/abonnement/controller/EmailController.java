package tn.esprit.abonnement.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.abonnement.dto.EmailRequest;
import tn.esprit.abonnement.services.EmailService;

import java.util.Map;

/**
 * REST controller for sending dynamic emails
 */
@RestController
@RequestMapping("/api/abonnements/email")
public class EmailController {

    private static final Logger logger = LoggerFactory.getLogger(EmailController.class);

    @Autowired
    private EmailService emailService;

    /**
     * Send a dynamic email with content controlled by the frontend
     *
     * POST /api/abonnements/email/send
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendEmail(@Valid @RequestBody EmailRequest request) {
        logger.info("Sending dynamic email to: {}", request.getToEmail());

        try {
            boolean sent = emailService.sendDynamicEmail(request);

            if (sent) {
                logger.info("Email sent successfully to: {}", request.getToEmail());
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Email sent successfully"));
            } else {
                logger.error("Failed to send email to: {}", request.getToEmail());
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of(
                                "success", false,
                                "message", "Failed to send email. Please try again."));
            }
        } catch (Exception e) {
            logger.error("Error sending email to {}: {}", request.getToEmail(), e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "An error occurred: " + e.getMessage()));
        }
    }
}
