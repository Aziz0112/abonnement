package tn.esprit.abonnement.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.abonnement.entity.PlanType;
import tn.esprit.abonnement.entity.SubscriptionPlan;
import tn.esprit.abonnement.entity.SubscriptionStatus;
import tn.esprit.abonnement.entity.UserSubscription;
import tn.esprit.abonnement.repository.SubscriptionPlanRepository;
import tn.esprit.abonnement.repository.UserSubscriptionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSubscriptionServiceTest {

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @InjectMocks
    private UserSubscriptionService userSubscriptionService;

    private SubscriptionPlan buildPlan(Long id, int durationDays) {
        return SubscriptionPlan.builder()
                .id(id).name(PlanType.STANDARD).price(9.99).durationDays(durationDays).description("desc").build();
    }

    private UserSubscription buildActiveSubscription(Long id, Long userId) {
        return UserSubscription.builder()
                .id(id)
                .userId(userId)
                .plan(buildPlan(1L, 30))
                .subscribedAt(LocalDateTime.now().minusDays(5))
                .expiresAt(LocalDateTime.now().plusDays(25))
                .status(SubscriptionStatus.ACTIVE)
                .build();
    }

    // ── bookSubscription() ────────────────────────────────────────────────────

    @Test
    void bookSubscription_withValidData_returnsActiveSubscription() {
        SubscriptionPlan plan = buildPlan(2L, 30);
        when(subscriptionPlanRepository.findById(2L)).thenReturn(Optional.of(plan));
        when(userSubscriptionRepository.findActiveSubscriptionByUserId(1L)).thenReturn(Optional.empty());
        when(userSubscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserSubscription result = userSubscriptionService.bookSubscription(1L, 2L);

        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getPlan()).isEqualTo(plan);
        assertThat(result.getExpiresAt()).isAfter(result.getSubscribedAt());
        assertThat(result.isAutoRenew()).isFalse();
        assertThat(result.isReminderSent()).isFalse();
    }

    @Test
    void bookSubscription_expiresAt_isCalculatedFromPlanDuration() {
        SubscriptionPlan plan = buildPlan(2L, 60);
        when(subscriptionPlanRepository.findById(2L)).thenReturn(Optional.of(plan));
        when(userSubscriptionRepository.findActiveSubscriptionByUserId(1L)).thenReturn(Optional.empty());
        when(userSubscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserSubscription result = userSubscriptionService.bookSubscription(1L, 2L);

        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(result.getSubscribedAt(), result.getExpiresAt());
        assertThat(daysDiff).isEqualTo(60);
    }

    @Test
    void bookSubscription_withNullUserId_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> userSubscriptionService.bookSubscription(null, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId");
    }

    @Test
    void bookSubscription_withZeroUserId_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> userSubscriptionService.bookSubscription(0L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId");
    }

    @Test
    void bookSubscription_withNullPlanId_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> userSubscriptionService.bookSubscription(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("planId");
    }

    @Test
    void bookSubscription_withNonExistentPlan_throwsIllegalArgumentException() {
        // Service fetches plan before checking active subscription
        when(subscriptionPlanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userSubscriptionService.bookSubscription(1L, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void bookSubscription_withExistingActiveSubscription_throwsIllegalStateException() {
        // Service fetches plan first, then checks for active subscription
        SubscriptionPlan plan = buildPlan(2L, 30);
        when(subscriptionPlanRepository.findById(2L)).thenReturn(Optional.of(plan));
        UserSubscription existing = buildActiveSubscription(1L, 1L);
        when(userSubscriptionRepository.findActiveSubscriptionByUserId(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userSubscriptionService.bookSubscription(1L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already has an active subscription");
    }

    // ── cancelSubscription() ──────────────────────────────────────────────────

    @Test
    void cancelSubscription_withActiveSubscription_setsStatusCancelled() {
        UserSubscription sub = buildActiveSubscription(1L, 1L);
        when(userSubscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
        when(userSubscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userSubscriptionService.cancelSubscription(1L);

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        verify(userSubscriptionRepository).save(sub);
    }

    @Test
    void cancelSubscription_withExpiredSubscription_throwsIllegalStateException() {
        UserSubscription sub = buildActiveSubscription(1L, 1L);
        sub.setStatus(SubscriptionStatus.EXPIRED);
        when(userSubscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> userSubscriptionService.cancelSubscription(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only active subscriptions can be cancelled");
    }

    @Test
    void cancelSubscription_withAlreadyCancelledSubscription_throwsIllegalStateException() {
        UserSubscription sub = buildActiveSubscription(1L, 1L);
        sub.setStatus(SubscriptionStatus.CANCELLED);
        when(userSubscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> userSubscriptionService.cancelSubscription(1L))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── toggleAutoRenew() ─────────────────────────────────────────────────────

    @Test
    void toggleAutoRenew_enablesAutoRenew() {
        UserSubscription sub = buildActiveSubscription(1L, 1L);
        sub.setAutoRenew(false);
        when(userSubscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
        when(userSubscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserSubscription result = userSubscriptionService.toggleAutoRenew(1L, true);

        assertThat(result.isAutoRenew()).isTrue();
        verify(userSubscriptionRepository).save(sub);
    }

    @Test
    void toggleAutoRenew_disablesAutoRenew() {
        UserSubscription sub = buildActiveSubscription(1L, 1L);
        sub.setAutoRenew(true);
        when(userSubscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
        when(userSubscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserSubscription result = userSubscriptionService.toggleAutoRenew(1L, false);

        assertThat(result.isAutoRenew()).isFalse();
    }

    @Test
    void toggleAutoRenew_withNonExistentId_throwsRuntimeException() {
        when(userSubscriptionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userSubscriptionService.toggleAutoRenew(99L, true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    // ── delete() — soft delete ────────────────────────────────────────────────

    @Test
    void delete_setsStatusToCancelled_insteadOfHardDelete() {
        UserSubscription sub = buildActiveSubscription(1L, 1L);
        when(userSubscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
        when(userSubscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userSubscriptionService.delete(1L);

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        verify(userSubscriptionRepository).save(sub);
    }

    @Test
    void delete_withNonExistentId_throwsRuntimeException() {
        when(userSubscriptionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userSubscriptionService.delete(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    // ── getById() ─────────────────────────────────────────────────────────────

    @Test
    void getById_withExistingId_returnsSubscription() {
        UserSubscription sub = buildActiveSubscription(1L, 5L);
        when(userSubscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));

        UserSubscription result = userSubscriptionService.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(5L);
    }

    @Test
    void getById_withNonExistentId_throwsRuntimeException() {
        when(userSubscriptionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userSubscriptionService.getById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    // ── getAll() ──────────────────────────────────────────────────────────────

    @Test
    void getAll_returnsAllSubscriptions() {
        List<UserSubscription> subs = List.of(
                buildActiveSubscription(1L, 1L),
                buildActiveSubscription(2L, 2L)
        );
        when(userSubscriptionRepository.findAll()).thenReturn(subs);

        List<UserSubscription> result = userSubscriptionService.getAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void getAll_withNoSubscriptions_returnsEmptyList() {
        when(userSubscriptionRepository.findAll()).thenReturn(List.of());

        List<UserSubscription> result = userSubscriptionService.getAll();

        assertThat(result).isEmpty();
    }

    // ── getActiveSubscriptionByUserId() ───────────────────────────────────────

    @Test
    void getActiveSubscriptionByUserId_whenActive_returnsPresent() {
        UserSubscription sub = buildActiveSubscription(1L, 1L);
        when(userSubscriptionRepository.findActiveSubscriptionByUserId(1L)).thenReturn(Optional.of(sub));

        Optional<UserSubscription> result = userSubscriptionService.getActiveSubscriptionByUserId(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void getActiveSubscriptionByUserId_whenNone_returnsEmpty() {
        when(userSubscriptionRepository.findActiveSubscriptionByUserId(2L)).thenReturn(Optional.empty());

        Optional<UserSubscription> result = userSubscriptionService.getActiveSubscriptionByUserId(2L);

        assertThat(result).isEmpty();
    }

    // ── update() ──────────────────────────────────────────────────────────────

    @Test
    void update_withExistingId_updatesAllFields() {
        UserSubscription existing = buildActiveSubscription(1L, 1L);
        when(userSubscriptionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userSubscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserSubscription updates = UserSubscription.builder()
                .userId(99L)
                .plan(buildPlan(3L, 365))
                .subscribedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusYears(1))
                .status(SubscriptionStatus.ACTIVE)
                .build();

        UserSubscription result = userSubscriptionService.update(1L, updates);

        assertThat(result.getUserId()).isEqualTo(99L);
        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void update_withNonExistentId_throwsRuntimeException() {
        when(userSubscriptionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userSubscriptionService.update(99L, buildActiveSubscription(99L, 1L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }
}
