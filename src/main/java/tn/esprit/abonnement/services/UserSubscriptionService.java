package tn.esprit.abonnement.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.abonnement.entity.SubscriptionPlan;
import tn.esprit.abonnement.entity.UserSubscription;
import tn.esprit.abonnement.repository.UserSubscriptionRepository;
import tn.esprit.abonnement.repository.SubscriptionPlanRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserSubscriptionService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    @Transactional
    public UserSubscription bookSubscription(Long userId, Long planId) {
        // Validate userId
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Valid userId is required");
        }

        // Validate planId
        if (planId == null || planId <= 0) {
            throw new IllegalArgumentException("Valid planId is required");
        }

        // Get plan
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("SubscriptionPlan not found with id: " + planId));

        // Check for active subscription
        Optional<UserSubscription> activeSubscription = 
                userSubscriptionRepository.findActiveSubscriptionByUserId(userId);
        
        if (activeSubscription.isPresent()) {
            throw new IllegalStateException("User already has an active subscription");
        }

        // Calculate expiry date
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(plan.getDurationDays());

        // Create subscription
        UserSubscription subscription = UserSubscription.builder()
                .userId(userId)
                .plan(plan)
                .subscribedAt(now)
                .expiresAt(expiresAt)
                .status(tn.esprit.abonnement.entity.SubscriptionStatus.ACTIVE)
                .reminderSent(false)
                .autoRenew(false)
                .build();

        return userSubscriptionRepository.save(subscription);
    }

    @Transactional
    public UserSubscription create(UserSubscription subscription) {
        // Validate userId
        if (subscription.getUserId() == null || subscription.getUserId() <= 0) {
            throw new IllegalArgumentException("Valid userId is required");
        }

        // Validate plan
        if (subscription.getPlan() == null || subscription.getPlan().getId() == null) {
            throw new IllegalArgumentException("Plan is required");
        }

        // Validate dates
        if (subscription.getSubscribedAt() == null) {
            subscription.setSubscribedAt(LocalDateTime.now());
        }

        if (subscription.getExpiresAt() == null) {
            SubscriptionPlan plan = subscription.getPlan();
            subscription.setExpiresAt(
                    subscription.getSubscribedAt().plusDays(plan.getDurationDays())
            );
        }

        // Validate expiry is after subscribed
        if (!subscription.getExpiresAt().isAfter(subscription.getSubscribedAt())) {
            throw new IllegalArgumentException("Expiry date must be after subscribed date");
        }

        // Set default status
        if (subscription.getStatus() == null) {
            subscription.setStatus(tn.esprit.abonnement.entity.SubscriptionStatus.ACTIVE);
        }

        // Feature 2: reset reminder flag on new subscription
        subscription.setReminderSent(false);

        return userSubscriptionRepository.save(subscription);
    }

    public UserSubscription update(Long id, UserSubscription subscription) {
        UserSubscription existing = userSubscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("UserSubscription not found with id: " + id));

        existing.setUserId(subscription.getUserId());
        existing.setPlan(subscription.getPlan());
        existing.setSubscribedAt(subscription.getSubscribedAt());
        existing.setExpiresAt(subscription.getExpiresAt());
        existing.setStatus(subscription.getStatus());

        return userSubscriptionRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        UserSubscription subscription = userSubscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("UserSubscription not found with id: " + id));
        
        // Soft delete: change status to CANCELLED instead of hard delete
        // This prevents foreign key constraint errors from invoices table
        subscription.setStatus(tn.esprit.abonnement.entity.SubscriptionStatus.CANCELLED);
        userSubscriptionRepository.save(subscription);
    }

    public UserSubscription getById(Long id) {
        return userSubscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("UserSubscription not found with id: " + id));
    }

    public List<UserSubscription> getAll() {
        return userSubscriptionRepository.findAll();
    }

    public Optional<UserSubscription> getActiveSubscriptionByUserId(Long userId) {
        return userSubscriptionRepository.findActiveSubscriptionByUserId(userId);
    }

    @Transactional
    public void cancelSubscription(Long subscriptionId) {
        UserSubscription subscription = getById(subscriptionId);

        if (subscription.getStatus() != tn.esprit.abonnement.entity.SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Only active subscriptions can be cancelled");
        }

        subscription.setStatus(tn.esprit.abonnement.entity.SubscriptionStatus.CANCELLED);
        userSubscriptionRepository.save(subscription);
    }

    /**
     * Feature 4: Toggle auto-renew on/off for a subscription.
     */
    @Transactional
    public UserSubscription toggleAutoRenew(Long subscriptionId, boolean enabled) {
        UserSubscription subscription = getById(subscriptionId);
        subscription.setAutoRenew(enabled);
        return userSubscriptionRepository.save(subscription);
    }
}
