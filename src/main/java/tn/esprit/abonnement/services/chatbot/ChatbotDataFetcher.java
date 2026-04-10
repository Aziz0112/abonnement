package tn.esprit.abonnement.services.chatbot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.abonnement.dto.SubscriptionSummary;
import tn.esprit.abonnement.entity.SubscriptionPlan;
import tn.esprit.abonnement.entity.UserSubscription;
import tn.esprit.abonnement.repository.SubscriptionPlanRepository;
import tn.esprit.abonnement.repository.UserSubscriptionRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotDataFetcher {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public SubscriptionSummary getUserSubscriptionSummary(Long userId) {
        UserSubscription subscription = userSubscriptionRepository.findActiveSubscriptionByUserId(userId)
                .orElse(null);

        if (subscription == null) {
            return SubscriptionSummary.builder()
                    .userId(userId)
                    .currentPlan("NONE")
                    .status("NOT_SUBSCRIBED")
                    .expiresAt(null)
                    .autoRenew(false)
                    .monthlyPrice(0.0)
                    .build();
        }

        return SubscriptionSummary.builder()
                .userId(userId)
                .currentPlan(subscription.getPlan().getName().name())
                .status(subscription.getStatus().name())
                .expiresAt(subscription.getExpiresAt())
                .autoRenew(subscription.isAutoRenew())
                .monthlyPrice(subscription.getPlan().getPrice())
                .build();
    }

    public Map<String, Object> getAvailablePlans() {
        List<SubscriptionPlan> plans = subscriptionPlanRepository.findAll();
        Map<String, Object> plansInfo = new HashMap<>();

        for (SubscriptionPlan plan : plans) {
            Map<String, Object> planDetails = new HashMap<>();
            planDetails.put("name", plan.getName().name());
            planDetails.put("price", plan.getPrice());
            planDetails.put("duration", plan.getDurationDays() + " days");
            planDetails.put("description", plan.getDescription());
            plansInfo.put(plan.getName().name(), planDetails);
        }

        return plansInfo;
    }

    public boolean isUserCloseToLimit(Long userId) {
        UserSubscription subscription = userSubscriptionRepository.findByUserId(userId)
                .orElse(null);

        if (subscription == null || subscription.getExpiresAt() == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDate = subscription.getExpiresAt();
        long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(now, expiryDate);

        return daysRemaining <= 3;
    }

    public String getPaymentStatus(Long userId) {
        UserSubscription subscription = userSubscriptionRepository.findByUserId(userId)
                .orElse(null);

        if (subscription == null) {
            return "No subscription found";
        }

        if (subscription.getStatus().name().equals("ACTIVE")) {
            return "Payment successful - Subscription active";
        } else if (subscription.getStatus().name().equals("PENDING_PAYMENT")) {
            return "Payment pending - Please complete payment";
        } else if (subscription.getStatus().name().equals("PAYMENT_FAILED")) {
            return "Payment failed - Please check your payment method";
        }

        return "Status: " + subscription.getStatus().name();
    }
}