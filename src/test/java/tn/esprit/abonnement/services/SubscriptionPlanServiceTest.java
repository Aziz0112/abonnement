package tn.esprit.abonnement.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.abonnement.entity.PlanType;
import tn.esprit.abonnement.entity.SubscriptionPlan;
import tn.esprit.abonnement.repository.SubscriptionPlanRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionPlanServiceTest {

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @InjectMocks
    private SubscriptionPlanService subscriptionPlanService;

    private SubscriptionPlan validPlan() {
        return SubscriptionPlan.builder()
                .name(PlanType.STANDARD)
                .price(9.99)
                .durationDays(30)
                .description("Standard monthly plan")
                .build();
    }

    // ── create() ──────────────────────────────────────────────────────────────

    @Test
    void create_withValidPlan_returnsSavedPlan() {
        SubscriptionPlan plan = validPlan();
        SubscriptionPlan saved = SubscriptionPlan.builder()
                .id(1L).name(PlanType.STANDARD).price(9.99).durationDays(30)
                .description("Standard monthly plan").build();
        when(subscriptionPlanRepository.save(plan)).thenReturn(saved);

        SubscriptionPlan result = subscriptionPlanService.create(plan);

        assertThat(result.getId()).isEqualTo(1L);
        verify(subscriptionPlanRepository).save(plan);
    }

    @Test
    void create_withNullPrice_throwsIllegalArgumentException() {
        SubscriptionPlan plan = validPlan();
        plan.setPrice(null);

        assertThatThrownBy(() -> subscriptionPlanService.create(plan))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Price is required");
    }

    @Test
    void create_withNegativePrice_throwsIllegalArgumentException() {
        SubscriptionPlan plan = validPlan();
        plan.setPrice(-5.0);

        assertThatThrownBy(() -> subscriptionPlanService.create(plan))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Price cannot be negative");
    }

    @Test
    void create_withNullDuration_throwsIllegalArgumentException() {
        SubscriptionPlan plan = validPlan();
        plan.setDurationDays(null);

        assertThatThrownBy(() -> subscriptionPlanService.create(plan))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duration is required");
    }

    @Test
    void create_withDurationLessThanOne_throwsIllegalArgumentException() {
        SubscriptionPlan plan = validPlan();
        plan.setDurationDays(0);

        assertThatThrownBy(() -> subscriptionPlanService.create(plan))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duration must be at least 1 day");
    }

    @Test
    void create_withBlankDescription_throwsIllegalArgumentException() {
        SubscriptionPlan plan = validPlan();
        plan.setDescription("   ");

        assertThatThrownBy(() -> subscriptionPlanService.create(plan))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Description is required");
    }

    @Test
    void create_withNullDescription_throwsIllegalArgumentException() {
        SubscriptionPlan plan = validPlan();
        plan.setDescription(null);

        assertThatThrownBy(() -> subscriptionPlanService.create(plan))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Description is required");
    }

    @Test
    void create_withNullName_throwsIllegalArgumentException() {
        SubscriptionPlan plan = validPlan();
        plan.setName(null);

        assertThatThrownBy(() -> subscriptionPlanService.create(plan))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plan name is required");
    }

    // ── update() ──────────────────────────────────────────────────────────────

    @Test
    void update_withValidData_updatesAndReturnsPlan() {
        SubscriptionPlan existing = SubscriptionPlan.builder()
                .id(1L).name(PlanType.STANDARD).price(9.99).durationDays(30).description("Old desc").build();
        SubscriptionPlan updates = SubscriptionPlan.builder()
                .price(19.99).durationDays(60).description("Updated desc").build();

        when(subscriptionPlanRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(subscriptionPlanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubscriptionPlan result = subscriptionPlanService.update(1L, updates);

        assertThat(result.getPrice()).isEqualTo(19.99);
        assertThat(result.getDurationDays()).isEqualTo(60);
        assertThat(result.getDescription()).isEqualTo("Updated desc");
    }

    @Test
    void update_withNonExistentId_throwsRuntimeException() {
        when(subscriptionPlanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionPlanService.update(99L, validPlan()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void update_withNegativePrice_throwsIllegalArgumentException() {
        SubscriptionPlan existing = SubscriptionPlan.builder()
                .id(1L).name(PlanType.STANDARD).price(9.99).durationDays(30).description("desc").build();
        when(subscriptionPlanRepository.findById(1L)).thenReturn(Optional.of(existing));

        SubscriptionPlan updates = SubscriptionPlan.builder().price(-10.0).build();

        assertThatThrownBy(() -> subscriptionPlanService.update(1L, updates))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Price cannot be negative");
    }

    @Test
    void update_withDurationLessThanOne_throwsIllegalArgumentException() {
        SubscriptionPlan existing = SubscriptionPlan.builder()
                .id(1L).name(PlanType.STANDARD).price(9.99).durationDays(30).description("desc").build();
        when(subscriptionPlanRepository.findById(1L)).thenReturn(Optional.of(existing));

        SubscriptionPlan updates = SubscriptionPlan.builder().durationDays(0).build();

        assertThatThrownBy(() -> subscriptionPlanService.update(1L, updates))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duration must be at least 1 day");
    }

    // ── delete() ──────────────────────────────────────────────────────────────

    @Test
    void delete_withExistingId_callsDeleteById() {
        when(subscriptionPlanRepository.existsById(1L)).thenReturn(true);

        subscriptionPlanService.delete(1L);

        verify(subscriptionPlanRepository).deleteById(1L);
    }

    @Test
    void delete_withNonExistentId_throwsRuntimeException() {
        when(subscriptionPlanRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> subscriptionPlanService.delete(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    // ── getById() ─────────────────────────────────────────────────────────────

    @Test
    void getById_withExistingId_returnsPlan() {
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .id(1L).name(PlanType.PREMIUM).price(29.99).durationDays(365).description("Premium plan").build();
        when(subscriptionPlanRepository.findById(1L)).thenReturn(Optional.of(plan));

        SubscriptionPlan result = subscriptionPlanService.getById(1L);

        assertThat(result.getName()).isEqualTo(PlanType.PREMIUM);
        assertThat(result.getPrice()).isEqualTo(29.99);
    }

    @Test
    void getById_withNonExistentId_throwsRuntimeException() {
        when(subscriptionPlanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionPlanService.getById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    // ── getAll() ──────────────────────────────────────────────────────────────

    @Test
    void getAll_returnsAllPlans() {
        List<SubscriptionPlan> plans = List.of(
                SubscriptionPlan.builder().id(1L).name(PlanType.FREEMIUM).price(0.0).durationDays(30).description("Free").build(),
                SubscriptionPlan.builder().id(2L).name(PlanType.STANDARD).price(9.99).durationDays(30).description("Std").build()
        );
        when(subscriptionPlanRepository.findAll()).thenReturn(plans);

        List<SubscriptionPlan> result = subscriptionPlanService.getAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo(PlanType.FREEMIUM);
        assertThat(result.get(1).getName()).isEqualTo(PlanType.STANDARD);
    }

    @Test
    void getAll_withNoPlans_returnsEmptyList() {
        when(subscriptionPlanRepository.findAll()).thenReturn(List.of());

        List<SubscriptionPlan> result = subscriptionPlanService.getAll();

        assertThat(result).isEmpty();
    }
}
