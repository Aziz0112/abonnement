package tn.esprit.abonnement.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.abonnement.dto.UserDTO;
import tn.esprit.abonnement.entity.SubscriptionStatus;
import tn.esprit.abonnement.entity.UserSubscription;
import tn.esprit.abonnement.feign.UserFeignClient;
import tn.esprit.abonnement.repository.UserSubscriptionRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpiryJobService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final EmailService emailService;
    private final UserFeignClient userFeignClient;
    private final InvoiceService invoiceService;

    /**
     * Scheduled job to process subscription renewals and expirations.
     * Runs every day at midnight (00:00).
     * Feature 4: handles auto-renewal logic before expiring.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void expireSubscriptions() {
        log.info("Starting subscription expiry/renewal job...");
        
        LocalDateTime now = LocalDateTime.now();
        List<UserSubscription> expiredSubscriptions = 
                userSubscriptionRepository.findExpiredSubscriptions(now);
        
        if (expiredSubscriptions.isEmpty()) {
            log.info("No expired subscriptions found.");
            return;
        }
        
        int expiredCount = 0;
        int renewedCount = 0;
        for (UserSubscription sub : expiredSubscriptions) {
            if (sub.isAutoRenew()) {
                try {
                    // Feature 4: Auto-renew — create new subscription with same plan
                    LocalDateTime renewStart = now;
                    LocalDateTime renewExpiry = now.plusDays(sub.getPlan().getDurationDays());

                    UserSubscription renewed = UserSubscription.builder()
                            .userId(sub.getUserId())
                            .plan(sub.getPlan())
                            .subscribedAt(renewStart)
                            .expiresAt(renewExpiry)
                            .status(SubscriptionStatus.ACTIVE)
                            .reminderSent(false)
                            .autoRenew(true)
                            .stripeCustomerId(sub.getStripeCustomerId())
                            .build();

                    renewed = userSubscriptionRepository.save(renewed);

                    // Create invoice for renewal
                    try {
                        invoiceService.createInvoiceForSubscription(renewed, null);
                    } catch (Exception e) {
                        log.error("Failed to create invoice for renewed subscription {}: {}", renewed.getId(), e.getMessage());
                    }

                    // Send renewal confirmation email
                    try {
                        UserDTO user = userFeignClient.getUserById(sub.getUserId());
                        String userName = user.getFirstName() + " " + user.getLastName();
                        emailService.sendExpiryReminderEmail(user.getEmail(), userName, renewed, 0);
                    } catch (Exception e) {
                        log.warn("Could not send renewal email for user {}: {}", sub.getUserId(), e.getMessage());
                    }

                    // Expire old subscription
                    sub.setStatus(SubscriptionStatus.EXPIRED);
                    userSubscriptionRepository.save(sub);
                    renewedCount++;

                    log.info("Auto-renewed subscription for user {} — new sub ID {}", sub.getUserId(), renewed.getId());
                    // TODO: Full Stripe auto-charge requires storing customer payment method ID (stripeCustomerId)
                } catch (Exception e) {
                    log.error("Auto-renewal failed for subscription {}, expiring instead: {}", sub.getId(), e.getMessage());
                    sub.setStatus(SubscriptionStatus.EXPIRED);
                    userSubscriptionRepository.save(sub);
                    expiredCount++;
                }
            } else {
                sub.setStatus(SubscriptionStatus.EXPIRED);
                userSubscriptionRepository.save(sub);
                expiredCount++;
                log.info("Expired subscription ID {} for user {}", sub.getId(), sub.getUserId());
            }
        }
        
        log.info("Subscription job completed. Expired: {}, Renewed: {}", expiredCount, renewedCount);
    }

    /**
     * Feature 2: Send expiry reminder emails for subscriptions expiring within 3 days.
     * Runs every day at 8:00 AM.
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void sendExpiryReminders() {
        log.info("Starting expiry reminder job...");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysFromNow = now.plusDays(3);
        List<UserSubscription> expiringSoon =
                userSubscriptionRepository.findSubscriptionsExpiringBetween(now, threeDaysFromNow);

        int sentCount = 0;
        for (UserSubscription sub : expiringSoon) {
            try {
                UserDTO user = userFeignClient.getUserById(sub.getUserId());
                String userName = user.getFirstName() + " " + user.getLastName();
                long daysLeft = java.time.Duration.between(now, sub.getExpiresAt()).toDays();
                if (daysLeft < 1) daysLeft = 1;

                boolean sent = emailService.sendExpiryReminderEmail(
                        user.getEmail(), userName, sub, daysLeft);

                if (sent) {
                    sub.setReminderSent(true);
                    userSubscriptionRepository.save(sub);
                    sentCount++;
                }
            } catch (Exception e) {
                log.warn("Could not send reminder for subscription {} (user {}): {}",
                        sub.getId(), sub.getUserId(), e.getMessage());
            }
        }
        log.info("Expiry reminder job completed. Sent {} reminders.", sentCount);
    }

    /**
     * Manual trigger for testing purposes.
     */
    @Transactional
    public int expireSubscriptionsNow() {
        log.info("Manually triggering subscription expiry...");
        
        LocalDateTime now = LocalDateTime.now();
        List<UserSubscription> expiredSubscriptions = 
                userSubscriptionRepository.findExpiredSubscriptions(now);
        
        int expiredCount = 0;
        for (UserSubscription subscription : expiredSubscriptions) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            userSubscriptionRepository.save(subscription);
            expiredCount++;
        }
        
        log.info("Manually expired {} subscriptions.", expiredCount);
        return expiredCount;
    }
}