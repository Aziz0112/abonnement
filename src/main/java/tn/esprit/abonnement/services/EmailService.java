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
import tn.esprit.abonnement.dto.EmailRequest;
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
         * Send a dynamic email with a premium HTML template.
         * Content is controlled by the frontend via EmailRequest fields.
         */
        public boolean sendDynamicEmail(EmailRequest request) {
                try {
                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                        helper.setFrom(fromEmail);
                        helper.setTo(request.getToEmail());
                        helper.setSubject(request.getSubject());

                        String emailBody = buildPremiumTemplate(request);
                        helper.setText(emailBody, true);

                        mailSender.send(message);

                        logger.info("Dynamic email sent successfully to: {}", request.getToEmail());
                        return true;

                } catch (MessagingException e) {
                        logger.error("Failed to send dynamic email to {}: {}", request.getToEmail(), e.getMessage(), e);
                        return false;
                } catch (Exception e) {
                        logger.error("Unexpected error sending dynamic email to {}: {}", request.getToEmail(),
                                        e.getMessage(), e);
                        return false;
                }
        }

        /**
         * Build a premium, modern HTML email template for subscription confirmations.
         * Uses inline CSS for maximum email client compatibility.
         */
        private String buildPremiumTemplate(EmailRequest request) {
                String userName = request.getUserName() != null ? escapeHtml(request.getUserName()) : "there";
                String planName = request.getPlanName() != null ? escapeHtml(request.getPlanName()) : "Premium";
                String amount = request.getAmount() != null ? escapeHtml(request.getAmount()) : "";
                String subscriptionDate = request.getSubscriptionDate() != null
                                ? escapeHtml(request.getSubscriptionDate())
                                : "";
                String expirationDate = request.getExpirationDate() != null ? escapeHtml(request.getExpirationDate())
                                : "";

                StringBuilder html = new StringBuilder();

                // DOCTYPE & wrapper
                html.append("<!DOCTYPE html>");
                html.append("<html lang='en'>");
                html.append("<head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'></head>");
                html.append("<body style='margin:0;padding:0;background-color:#f0f2f5;font-family:\"Segoe UI\",Roboto,\"Helvetica Neue\",Arial,sans-serif;'>");

                // Outer table for centering
                html.append("<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='background-color:#f0f2f5;padding:40px 20px;'>");
                html.append("<tr><td align='center'>");

                // Inner container (max 600px)
                html.append("<table role='presentation' width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);'>");

                // ─── HEADER with gradient ───
                html.append("<tr><td style='background:linear-gradient(135deg,#6366f1 0%,#8b5cf6 50%,#a855f7 100%);padding:40px 40px 32px;text-align:center;'>");
                html.append("<table role='presentation' width='100%' cellpadding='0' cellspacing='0'><tr><td align='center'>");
                // Logo / Brand name
                html.append("<h1 style='margin:0 0 8px;font-size:32px;font-weight:800;color:#ffffff;letter-spacing:-0.5px;'>🌍 ")
                                .append(platformName).append("</h1>");
                html.append("<p style='margin:0;font-size:14px;color:rgba(255,255,255,0.85);letter-spacing:0.5px;text-transform:uppercase;'>Subscription Confirmation</p>");
                html.append("</td></tr></table>");
                html.append("</td></tr>");

                // ─── SUCCESS BADGE ───
                html.append("<tr><td style='background-color:#ffffff;padding:32px 40px 0;text-align:center;'>");
                html.append("<div style='display:inline-block;background:linear-gradient(135deg,#22c55e,#16a34a);border-radius:50%;width:64px;height:64px;line-height:64px;text-align:center;margin-bottom:16px;'>");
                html.append("<span style='font-size:32px;color:#ffffff;'>✓</span>");
                html.append("</div>");
                html.append("<h2 style='margin:0 0 8px;font-size:24px;font-weight:700;color:#1e293b;'>Payment Successful!</h2>");
                html.append("<p style='margin:0 0 24px;font-size:15px;color:#64748b;line-height:1.6;'>Hey <strong style=\"color:#1e293b;\">")
                                .append(userName)
                                .append("</strong>, your subscription is now active. Welcome aboard!</p>");
                html.append("</td></tr>");

                // ─── SUBSCRIPTION DETAILS CARD ───
                html.append("<tr><td style='background-color:#ffffff;padding:0 40px 32px;'>");
                html.append("<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='background-color:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;overflow:hidden;'>");

                // Card header
                html.append("<tr><td style='background:linear-gradient(135deg,#6366f1,#8b5cf6);padding:14px 24px;'>");
                html.append("<h3 style='margin:0;font-size:14px;font-weight:600;color:#ffffff;letter-spacing:0.5px;text-transform:uppercase;'>Subscription Details</h3>");
                html.append("</td></tr>");

                // Card body — detail rows
                html.append("<tr><td style='padding:20px 24px;'>");
                html.append("<table role='presentation' width='100%' cellpadding='0' cellspacing='0'>");

                // Plan row
                html.append("<tr>");
                html.append("<td style='padding:10px 0;border-bottom:1px solid #e2e8f0;font-size:14px;color:#64748b;width:40%;'>Plan</td>");
                html.append("<td style='padding:10px 0;border-bottom:1px solid #e2e8f0;font-size:14px;font-weight:600;color:#1e293b;text-align:right;'>");
                html.append("<span style='display:inline-block;background:linear-gradient(135deg,#6366f1,#a855f7);color:#fff;padding:4px 14px;border-radius:20px;font-size:13px;font-weight:600;'>")
                                .append(planName).append("</span>");
                html.append("</td></tr>");

                // Amount row (only if provided)
                if (!amount.isEmpty()) {
                        html.append("<tr>");
                        html.append("<td style='padding:10px 0;border-bottom:1px solid #e2e8f0;font-size:14px;color:#64748b;'>Amount Paid</td>");
                        html.append("<td style='padding:10px 0;border-bottom:1px solid #e2e8f0;font-size:20px;font-weight:700;color:#1e293b;text-align:right;'>")
                                        .append(amount).append("</td>");
                        html.append("</tr>");
                }

                // Start date row (only if provided)
                if (!subscriptionDate.isEmpty()) {
                        html.append("<tr>");
                        html.append("<td style='padding:10px 0;border-bottom:1px solid #e2e8f0;font-size:14px;color:#64748b;'>Start Date</td>");
                        html.append("<td style='padding:10px 0;border-bottom:1px solid #e2e8f0;font-size:14px;font-weight:600;color:#1e293b;text-align:right;'>")
                                        .append(subscriptionDate).append("</td>");
                        html.append("</tr>");
                }

                // Expiration date row (only if provided)
                if (!expirationDate.isEmpty()) {
                        html.append("<tr>");
                        html.append("<td style='padding:10px 0;border-bottom:1px solid #e2e8f0;font-size:14px;color:#64748b;'>Expires On</td>");
                        html.append("<td style='padding:10px 0;border-bottom:1px solid #e2e8f0;font-size:14px;font-weight:600;color:#1e293b;text-align:right;'>")
                                        .append(expirationDate).append("</td>");
                        html.append("</tr>");
                }

                // Status row
                html.append("<tr>");
                html.append("<td style='padding:10px 0;font-size:14px;color:#64748b;'>Status</td>");
                html.append("<td style='padding:10px 0;font-size:14px;font-weight:700;color:#22c55e;text-align:right;'>● Active</td>");
                html.append("</tr>");

                // Payment method row
                html.append("<tr>");
                html.append("<td style='padding:10px 0;font-size:14px;color:#64748b;'>Payment Method</td>");
                html.append("<td style='padding:10px 0;font-size:14px;font-weight:600;color:#1e293b;text-align:right;'>💳 Stripe</td>");
                html.append("</tr>");

                html.append("</table>");
                html.append("</td></tr>");
                html.append("</table>"); // end card
                html.append("</td></tr>");

                // ─── CTA BUTTON ───
                html.append("<tr><td style='background-color:#ffffff;padding:0 40px 32px;text-align:center;'>");
                html.append("<a href='").append(frontendUrl).append(
                                "/user/subscription' style='display:inline-block;background:linear-gradient(135deg,#6366f1,#8b5cf6);color:#ffffff;text-decoration:none;padding:14px 40px;border-radius:10px;font-size:15px;font-weight:600;letter-spacing:0.3px;'>Start Learning Now →</a>");
                html.append("</td></tr>");

                // ─── DIVIDER ───
                html.append("<tr><td style='background-color:#ffffff;padding:0 40px;'>");
                html.append("<hr style='border:none;border-top:1px solid #e2e8f0;margin:0;'>");
                html.append("</td></tr>");

                // ─── FOOTER ───
                html.append("<tr><td style='background-color:#ffffff;padding:24px 40px 32px;text-align:center;border-radius:0 0 16px 16px;'>");
                html.append("<p style='margin:0 0 8px;font-size:13px;color:#94a3b8;'>Thank you for choosing <strong style=\"color:#6366f1;\">")
                                .append(platformName).append("</strong></p>");
                html.append("<p style='margin:0 0 12px;font-size:12px;color:#94a3b8;'>Need help? Reach us at <a href='mailto:")
                                .append(supportEmail)
                                .append("' style='color:#6366f1;text-decoration:none;font-weight:600;'>")
                                .append(supportEmail).append("</a></p>");
                html.append("<p style='margin:0;font-size:11px;color:#cbd5e1;'>© 2026 ").append(platformName)
                                .append(". All rights reserved.</p>");
                html.append("</td></tr>");

                html.append("</table>"); // end inner container
                html.append("</td></tr></table>"); // end outer table
                html.append("</body></html>");

                return html.toString();
        }

        /**
         * Build HTML email body (legacy — used by sendSubscriptionConfirmation)
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
                if (date == null)
                        return "N/A";
                return date.format(java.time.format.DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
        }

        /**
         * Escape HTML safely
         */
        private String escapeHtml(String text) {
                if (text == null)
                        return "";

                return text.replace("&", "&amp;")
                                .replace("<", "&lt;")
                                .replace(">", "&gt;")
                                .replace("\"", "&quot;")
                                .replace("'", "&#39;");
        }

        /**
         * Send expiry reminder email to warn users their subscription is expiring soon
         */
        public boolean sendExpiryReminderEmail(String toEmail, String userName,
                        UserSubscription sub, long daysLeft) {
                try {
                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                        helper.setFrom(fromEmail);
                        helper.setTo(toEmail);
                        helper.setSubject("Your " + platformName + " subscription expires in " + daysLeft + " day(s)!");

                        String emailBody = buildExpiryReminderBody(userName, sub, daysLeft);
                        helper.setText(emailBody, true);

                        mailSender.send(message);

                        logger.info("Expiry reminder email sent successfully to: {}", toEmail);
                        return true;

                } catch (MessagingException e) {
                        logger.error("Failed to send expiry reminder email to {}: {}", toEmail, e.getMessage(), e);
                        return false;
                } catch (Exception e) {
                        logger.error("Unexpected error sending expiry reminder email to {}: {}", toEmail,
                                        e.getMessage(), e);
                        return false;
                }
        }

        private String buildExpiryReminderBody(String userName, UserSubscription subscription, long daysLeft) {
                String safeName = escapeHtml(userName != null ? userName : "there");
                String planName = subscription.getPlan() != null
                                ? escapeHtml(subscription.getPlan().getName().name())
                                : "Premium";
                String expiryDate = formatDate(subscription.getExpiresAt());

                StringBuilder html = new StringBuilder();
                html.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'></head>");
                html.append("<body style='margin:0;padding:0;background-color:#f0f2f5;font-family:Arial,sans-serif;'>");
                html.append("<table width='100%' cellpadding='0' cellspacing='0' style='background-color:#f0f2f5;padding:40px 20px;'>");
                html.append("<tr><td align='center'>");
                html.append("<table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);'>");
                html.append("<tr><td style='background:linear-gradient(135deg,#f59e0b,#ef4444);padding:40px;text-align:center;'>");
                html.append("<h1 style='margin:0;font-size:32px;font-weight:800;color:#fff;'>").append(platformName).append("</h1>");
                html.append("<p style='margin:8px 0 0;font-size:14px;color:rgba(255,255,255,0.85);text-transform:uppercase;'>Subscription Expiring Soon</p>");
                html.append("</td></tr>");
                html.append("<tr><td style='background:#fff;padding:32px 40px;text-align:center;'>");
                html.append("<h2 style='margin:0 0 8px;font-size:24px;color:#1e293b;'>Expiring in ").append(daysLeft).append(" day(s)</h2>");
                html.append("<p style='margin:0 0 24px;font-size:15px;color:#64748b;'>Hey <strong>").append(safeName).append("</strong>, your <strong>").append(planName).append("</strong> plan expires on <strong>").append(expiryDate).append("</strong>.</p>");
                html.append("<a href='http://localhost:4200/subscriptions' style='display:inline-block;background:linear-gradient(135deg,#6366f1,#8b5cf6);color:#fff;text-decoration:none;padding:14px 40px;border-radius:10px;font-size:15px;font-weight:600;'>Renew Now</a>");
                html.append("</td></tr>");
                html.append("<tr><td style='background:#fff;padding:24px 40px;text-align:center;border-radius:0 0 16px 16px;'>");
                html.append("<p style='margin:0;font-size:13px;color:#94a3b8;'>Need help? <a href='mailto:").append(supportEmail).append("' style='color:#6366f1;'>").append(supportEmail).append("</a></p>");
                html.append("</td></tr></table></td></tr></table></body></html>");
                return html.toString();
        }
}