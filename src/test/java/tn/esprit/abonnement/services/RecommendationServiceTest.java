package tn.esprit.abonnement.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.abonnement.dto.RecommendationDTO;
import tn.esprit.abonnement.entity.PlanType;
import tn.esprit.abonnement.entity.SubscriptionPlan;
import tn.esprit.abonnement.entity.SubscriptionStatus;
import tn.esprit.abonnement.entity.UserSubscription;
import tn.esprit.abonnement.repository.UserSubscriptionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @InjectMocks
    private RecommendationService recommendationService;

    private SubscriptionPlan plan(PlanType type) {
        return SubscriptionPlan.builder()
                .id(1L).name(type).price(9.99).durationDays(30).description("desc").build();
    }

    private UserSubscription activeSubWithPlan(PlanType type, LocalDateTime subscribedAt, LocalDateTime expiresAt) {
        return UserSubscription.builder()
                .id(1L)
                .userId(1L)
                .plan(plan(type))
                .subscribedAt(subscribedAt)
                .expiresAt(expiresAt)
                .status(SubscriptionStatus.ACTIVE)
                .build();
    }

    private UserSubscription inactiveSub(PlanType type, SubscriptionStatus status) {
        return UserSubscription.builder()
                .id(1L)
                .userId(1L)
                .plan(plan(type))
                .subscribedAt(LocalDateTime.now().minusDays(60))
                .expiresAt(LocalDateTime.now().minusDays(30))
                .status(status)
                .build();
    }

    // ── State 1: NEW_USER — no subscription history ───────────────────────────

    @Test
    void getRecommendation_withNoHistory_returnsStandardAndNewUserState() {
        when(userSubscriptionRepository.findByUserId(1L)).thenReturn(List.of());

        RecommendationDTO result = recommendationService.getRecommendationForUser(1L);

        assertThat(result.getRecommendedPlan()).isEqualTo(PlanType.STANDARD);
        assertThat(result.getState()).isEqualTo("NEW_USER");
        assertThat(result.getReason()).isNotBlank();
    }

    // ── State 5: EXPIRING_SOON — active plan expires in < 7 days ─────────────

    @Test
    void getRecommendation_withStandardExpiringIn3Days_returnsExpiringState() {
        LocalDateTime now = LocalDateTime.now();
        UserSubscription sub = activeSubWithPlan(PlanType.STANDARD, now.minusDays(27), now.plusDays(3));

        when(userSubscriptionRepository.findByUserId(1L)).thenReturn(List.of(sub));
        when(userSubscriptionRepository.findActiveSubscriptionByUserId(1L)).thenReturn(Optional.of(sub));

        RecommendationDTO result = recommendationService.getRecommendationForUser(1L);

        assertThat(result.getState()).isEqualTo("EXPIRING_SOON");
        assertThat(result.getRecommendedPlan()).isEqualTo(PlanType.PREMIUM);
    }

    @Test
    void getRecommendation_withPremiumExpiringIn3Days_doesNotReturnExpiringState() {
        // Premium + expiring soon should NOT return EXPIRING_SOON (already on top tier)
        LocalDateTime now = LocalDateTime.now();
        UserSubscription sub = activeSubWithPlan(PlanType.PREMIUM, now.minusDays(27), now.plusDays(3));

        when(userSubscriptionRepository.findByUserId(1L)).thenReturn(List.of(sub));
        when(userSubscriptionRepository.findActiveSubscriptionByUserId(1L)).thenReturn(Optional.of(sub));

        RecommendationDTO result = recommendationService.getRecommendationForUser(1L);

        assertThat(result.getState()).isNotEqualTo("EXPIRING_SOON");
    }

    // ── State 2: LONG_FREEMIUM — freemium for > 7 days ───────────────────────

    @Test
    void getRecommendation_withFreemiumFor10Days_returnsLongFreemiumState() {
        LocalDateTime now = LocalDateTime.now();
        UserSubscription sub = activeSubWithPlan(PlanType.FREEMIUM, now.minusDays(10), now.plusDays(20));

        when(userSubscriptionRepository.findByUserId(1L)).thenReturn(List.of(sub));
        when(userSubscriptionRepository.findActiveSubscriptionByUserId(1L)).thenReturn(Optional.of(sub));

        RecommendationDTO result = recommendationService.getRecommendationForUser(1L);

        assertThat(result.getState()).isEqualTo("LONG_FREEMIUM");
        assertThat(result.getRecommendedPlan()).isEqualTo(PlanType.STANDARD);
    }

    @Test
    void getRecommendation_withFreemiumFor3Days_doesNotReturnLongFreemiumState() {
        LocalDateTime now = LocalDateTime.now();
        UserSubscription sub = activeSubWithPlan(PlanType.FREEMIUM, now.minusDays(3), now.plusDays(27));

        when(userSubscriptionRepository.findByUserId(1L)).thenReturn(List.of(sub));
        when(userSubscriptionRepository.findActiveSubscriptionByUserId(1L)).thenReturn(Optional.of(sub));

        RecommendationDTO result = recommendationService.getRecommendationForUser(1L);

        assertThat(result.getState()).isNotEqualTo("LONG_FREEMIUM");
    }

    // ── State: ACTIVE_UPSELL — active standard, suggest premium ──────────────

    @Test
    void getRecommendation_withActiveStandard_returnsActiveUpsellState() {
        LocalDateTime now = LocalDateTime.now();
        // Standard, subscribed long ago (no expiring soon trigger), not freemium
        UserSubscription sub = activeSubWithPlan(PlanType.STANDARD, now.minusDays(15), now.plusDays(15));

        when(userSubscriptionRepository.findByUserId(1L)).thenReturn(List.of(sub));
        when(userSubscriptionRepository.findActiveSubscriptionByUserId(1L)).thenReturn(Optional.of(sub));

        RecommendationDTO result = recommendationService.getRecommendationForUser(1L);

        assertThat(result.getState()).isEqualTo("ACTIVE_UPSELL");
        assertThat(result.getRecommendedPlan()).isEqualTo(PlanType.PREMIUM);
    }

    // ── State: ACTIVE_TOP_TIER — already on PREMIUM ───────────────────────────

    @Test
    void getRecommendation_withActivePremium_returnsActiveTopTierState() {
        LocalDateTime now = LocalDateTime.now();
        UserSubscription sub = activeSubWithPlan(PlanType.PREMIUM, now.minusDays(15), now.plusDays(350));

        when(userSubscriptionRepository.findByUserId(1L)).thenReturn(List.of(sub));
        when(userSubscriptionRepository.findActiveSubscriptionByUserId(1L)).thenReturn(Optional.of(sub));

        RecommendationDTO result = recommendationService.getRecommendationForUser(1L);

        assertThat(result.getState()).isEqualTo("ACTIVE_TOP_TIER");
        assertThat(result.getRecommendedPlan()).isEqualTo(PlanType.PREMIUM);
    }

    // ── State 4: WIN_BACK — cancelled subscription ────────────────────────────

    @Test
    void getRecommendation_withCancelledStandard_returnsWinBackWithPremium() {
        UserSubscription cancelled = inactiveSub(PlanType.STANDARD, SubscriptionStatus.CANCELLED);

        when(userSubscriptionRepository.findByUserId(1L)).thenReturn(List.of(cancelled));
        when(userSubscriptionRepository.findActiveSubscriptionByUserId(1L)).thenReturn(Optional.empty());

        RecommendationDTO result = recommendationService.getRecommendationForUser(1L);

        assertThat(result.getState()).isEqualTo("WIN_BACK");
        assertThat(result.getRecommendedPlan()).isEqualTo(PlanType.PREMIUM);
    }

    @Test
    void getRecommendation_withCancelledFreemium_returnsWinBackWithStandard() {
        UserSubscription cancelled = inactiveSub(PlanType.FREEMIUM, SubscriptionStatus.CANCELLED);

        when(userSubscriptionRepository.findByUserId(1L)).thenReturn(List.of(cancelled));
        when(userSubscriptionRepository.findActiveSubscriptionByUserId(1L)).thenReturn(Optional.empty());

        RecommendationDTO result = recommendationService.getRecommendationForUser(1L);

        assertThat(result.getState()).isEqualTo("WIN_BACK");
        assertThat(result.getRecommendedPlan()).isEqualTo(PlanType.STANDARD);
    }

    // ── State 3: RETURNING — expired subscription ─────────────────────────────

    @Test
    void getRecommendation_withExpiredSubscription_returnsReturningState() {
        UserSubscription expired = inactiveSub(PlanType.STANDARD, SubscriptionStatus.EXPIRED);

        when(userSubscriptionRepository.findByUserId(1L)).thenReturn(List.of(expired));
        when(userSubscriptionRepository.findActiveSubscriptionByUserId(1L)).thenReturn(Optional.empty());

        RecommendationDTO result = recommendationService.getRecommendationForUser(1L);

        assertThat(result.getState()).isEqualTo("RETURNING");
        assertThat(result.getRecommendedPlan()).isEqualTo(PlanType.STANDARD);
    }

    @Test
    void getRecommendation_withExpiredPremium_returnsReturningWithPremium() {
        UserSubscription expired = inactiveSub(PlanType.PREMIUM, SubscriptionStatus.EXPIRED);

        when(userSubscriptionRepository.findByUserId(1L)).thenReturn(List.of(expired));
        when(userSubscriptionRepository.findActiveSubscriptionByUserId(1L)).thenReturn(Optional.empty());

        RecommendationDTO result = recommendationService.getRecommendationForUser(1L);

        assertThat(result.getState()).isEqualTo("RETURNING");
        assertThat(result.getRecommendedPlan()).isEqualTo(PlanType.PREMIUM);
    }
}
