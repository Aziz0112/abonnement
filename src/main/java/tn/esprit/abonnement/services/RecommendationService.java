package tn.esprit.abonnement.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.abonnement.dto.RecommendationDTO;
import tn.esprit.abonnement.entity.PlanType;
import tn.esprit.abonnement.entity.SubscriptionStatus;
import tn.esprit.abonnement.entity.UserSubscription;
import tn.esprit.abonnement.repository.UserSubscriptionRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserSubscriptionRepository userSubscriptionRepository;

    public RecommendationDTO getRecommendationForUser(Long userId) {
        List<UserSubscription> history = userSubscriptionRepository.findByUserId(userId);

        // State 1 — Never subscribed
        if (history.isEmpty()) {
            return RecommendationDTO.builder()
                    .recommendedPlan(PlanType.STANDARD)
                    .reason("Most popular plan — perfect to get started.")
                    .state("NEW_USER")
                    .build();
        }

        Optional<UserSubscription> activeOpt = userSubscriptionRepository.findActiveSubscriptionByUserId(userId);

        if (activeOpt.isPresent()) {
            UserSubscription active = activeOpt.get();
            PlanType currentPlan = active.getPlan().getName();
            LocalDateTime now = LocalDateTime.now();

            // State 5 — Active, expires soon
            if (active.getExpiresAt() != null) {
                long daysToExpiry = Duration.between(now, active.getExpiresAt()).toDays();
                if (daysToExpiry >= 0 && daysToExpiry < 7 && currentPlan != PlanType.PREMIUM) {
                    return RecommendationDTO.builder()
                            .recommendedPlan(PlanType.PREMIUM)
                            .reason("Your plan expires in " + daysToExpiry + " day(s) — upgrade to Premium for more.")
                            .state("EXPIRING_SOON")
                            .build();
                }
            }

            // State 2 — Active FREEMIUM > 7 days
            if (currentPlan == PlanType.FREEMIUM && active.getSubscribedAt() != null) {
                long daysOnPlan = Duration.between(active.getSubscribedAt(), now).toDays();
                if (daysOnPlan > 7) {
                    return RecommendationDTO.builder()
                            .recommendedPlan(PlanType.STANDARD)
                            .reason("You've been on Basic for " + daysOnPlan + " days — unlock more with Plus.")
                            .state("LONG_FREEMIUM")
                            .build();
                }
            }

            // Active but no specific trigger — suggest next tier if not PREMIUM
            PlanType next = nextTier(currentPlan);
            if (next != currentPlan) {
                return RecommendationDTO.builder()
                        .recommendedPlan(next)
                        .reason("Enjoying your plan? Level up for even more features.")
                        .state("ACTIVE_UPSELL")
                        .build();
            }

            return RecommendationDTO.builder()
                    .recommendedPlan(PlanType.PREMIUM)
                    .reason("You're on our top plan — keep enjoying Premium perks.")
                    .state("ACTIVE_TOP_TIER")
                    .build();
        }

        // Not currently active — look at most recent past subscription
        UserSubscription last = history.stream()
                .max(Comparator.comparing(UserSubscription::getId))
                .orElse(history.get(history.size() - 1));

        PlanType lastPlan = last.getPlan().getName();

        // State 4 — CANCELLED
        if (last.getStatus() == SubscriptionStatus.CANCELLED) {
            PlanType next = nextTier(lastPlan);
            return RecommendationDTO.builder()
                    .recommendedPlan(next)
                    .reason("Give it another try with even more features.")
                    .state("WIN_BACK")
                    .build();
        }

        // State 3 — EXPIRED
        if (last.getStatus() == SubscriptionStatus.EXPIRED) {
            return RecommendationDTO.builder()
                    .recommendedPlan(lastPlan)
                    .reason("Welcome back — pick up where you left off.")
                    .state("RETURNING")
                    .build();
        }

        // Fallback
        return RecommendationDTO.builder()
                .recommendedPlan(PlanType.STANDARD)
                .reason("Our most popular plan.")
                .state("DEFAULT")
                .build();
    }

    private PlanType nextTier(PlanType current) {
        return switch (current) {
            case FREEMIUM -> PlanType.STANDARD;
            case STANDARD -> PlanType.PREMIUM;
            case PREMIUM -> PlanType.PREMIUM;
        };
    }
}
