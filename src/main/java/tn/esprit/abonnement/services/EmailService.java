package tn.esprit.abonnement.services;

import jakarta.activation.DataSource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import tn.esprit.abonnement.entity.UserSubscription;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.platform.name}")
    private String platformName;

    @Value("${app.support.email}")
    private String supportEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * Send subscription confirmation email with PDF attachment
     */
    public boolean sendSubscriptionConfirmation(String toEmail,
                                                String userName,
                                                UserSubscription subscription,
                                                byte[] pdfBytes) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Your Subscription Has Been Activated 🎉");

            String emailBody = buildEmailBody(userName, subscription);
            helper.setText(emailBody, true);

            // Attach PDF
            DataSource dataSource = new ByteArrayDataSource(pdfBytes, "application/pdf");
            helper.addAttachment("payment-receipt.pdf", dataSource);

            mailSender.send(message);

            logger.info("Subscription confirmation email sent successfully to: {}", toEmail);
            return true;

        } catch (MessagingException e) {
            logger.error("Failed to send email to {}: {}", toEmail, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error sending email to {}: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Build HTML email body
     */
    private String buildEmailBody(String userName, UserSubscription subscription) {

        StringBuilder body = new StringBuilder();

        body.append("<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>");

        // Greeting
        body.append("<h2 style='color: #2c3e50;'>Hello ")
                .append(escapeHtml(userName))
                .append(",</h2>");

        body.append("<p>Great news! 🎉 Your subscription has been activated.</p>");

        // Subscription Details
        body.append("<div style='background-color: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;'>");
        body.append("<h3 style='color: #2c3e50;'>Subscription Details:</h3>");
        body.append("<ul style='list-style: none; padding: 0;'>");

        body.append("<li><strong>Plan:</strong> ")
                .append(escapeHtml(subscription.getPlan().getName().name()))
                .append("</li>");

        body.append("<li><strong>Duration:</strong> ")
                .append(subscription.getPlan().getDurationDays())
                .append(" days</li>");

        body.append("<li><strong>Active from:</strong> ")
                .append(formatDate(subscription.getSubscribedAt()))
                .append("</li>");

        body.append("<li><strong>Expiration Date:</strong> ")
                .append(formatDate(subscription.getExpiresAt()))
                .append("</li>");

        body.append("<li><strong>Status:</strong> <span style='color: green; font-weight: bold;'>ACTIVE</span></li>");
        body.append("</ul>");
        body.append("</div>");

        // Payment Details
        body.append("<div style='background-color: #e8f4f8; padding: 20px; border-radius: 8px; margin: 20px 0;'>");
        body.append("<h3 style='color: #2c3e50;'>Payment Details:</h3>");
        body.append("<ul style='list-style: none; padding: 0;'>");

        body.append("<li><strong>Amount:</strong> ")
                .append(subscription.getPlan().getPrice())
                .append("</li>");

        body.append("<li><strong>Payment Method:</strong> Simulated Card</li>");
        body.append("<li><strong>Payment Status:</strong> <span style='color: green; font-weight: bold;'>SUCCESS</span></li>");
        body.append("</ul>");
        body.append("</div>");

        body.append("<p>Your payment receipt is attached as a PDF file.</p>");
        body.append("<p>Thank you for subscribing to ").append(platformName).append("!</p>");

        body.append("<hr style='border-top: 1px solid #ddd;'>");
        body.append("<p>Best regards,<br><strong>")
                .append(platformName)
                .append(" Team</strong></p>");

        body.append("<p style='font-size: 0.9em;'>Need help? Contact us at ")
                .append("<a href='mailto:")
                .append(supportEmail)
                .append("'>")
                .append(supportEmail)
                .append("</a></p>");

        body.append("</body></html>");

        return body.toString();
    }

    /**
     * Format date nicely
     */
    private String formatDate(java.time.LocalDateTime date) {
        if (date == null) return "N/A";
        return date.format(java.time.format.DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
    }

    /**
     * Escape HTML safely
     */
    private String escapeHtml(String text) {
        if (text == null) return "";

        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}