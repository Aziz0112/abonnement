package tn.esprit.abonnement.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.abonnement.entity.SubscriptionStatus;
import tn.esprit.abonnement.entity.UserSubscription;
import tn.esprit.abonnement.repository.UserSubscriptionRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpiryJobService {

    private final UserSubscriptionRepository userSubscriptionRepository;

    /**
     * Scheduled job to expire subscriptions that have passed their expiry date.
     * Runs every day at midnight (00:00).
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void expireSubscriptions() {
        log.info("Starting subscription expiry job...");
        
        LocalDateTime now = LocalDateTime.now();
        List<UserSubscription> expiredSubscriptions = 
                userSubscriptionRepository.findExpiredSubscriptions(now);
        
        if (expiredSubscriptions.isEmpty()) {
            log.info("No expired subscriptions found.");
            return;
        }
        
        int expiredCount = 0;
        for (UserSubscription subscription : expiredSubscriptions) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            userSubscriptionRepository.save(subscription);
            expiredCount++;
            
            log.info("Expired subscription ID {} for user {}", 
                    subscription.getId(), subscription.getUserId());
        }
        
        log.info("Subscription expiry job completed. Expired {} subscriptions.", expiredCount);
    }

    /**
     * Manual trigger for testing purposes.
     * Can be called via an endpoint to test the expiry logic.
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