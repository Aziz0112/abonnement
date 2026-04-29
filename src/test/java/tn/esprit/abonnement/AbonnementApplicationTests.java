package tn.esprit.abonnement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.abonnement.entity.PlanType;
import tn.esprit.abonnement.entity.SubscriptionPlan;
import tn.esprit.abonnement.repository.SubscriptionPlanRepository;
import tn.esprit.abonnement.services.SubscriptionPlanService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbonnementApplicationTests {

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @InjectMocks
    private SubscriptionPlanService subscriptionPlanService;

    @Test
    void testCreatePlan_success() {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setName(PlanType.STANDARD);
        plan.setPrice(10.0);
        plan.setDurationDays(30);
        plan.setDescription("Standard plan");

        when(subscriptionPlanRepository.save(plan)).thenReturn(plan);

        SubscriptionPlan result = subscriptionPlanService.create(plan);

        assertNotNull(result);
        assertEquals(PlanType.STANDARD, result.getName());
        verify(subscriptionPlanRepository, times(1)).save(plan);
    }

    @Test
    void testCreatePlan_nullPrice_throwsException() {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setName(PlanType.FREEMIUM);
        plan.setPrice(null);
        plan.setDurationDays(30);
        plan.setDescription("Freemium plan");

        assertThrows(IllegalArgumentException.class, () -> subscriptionPlanService.create(plan));
    }

    @Test
    void testCreatePlan_negativePrice_throwsException() {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setName(PlanType.STANDARD);
        plan.setPrice(-5.0);
        plan.setDurationDays(30);
        plan.setDescription("Standard plan");

        assertThrows(IllegalArgumentException.class, () -> subscriptionPlanService.create(plan));
    }

    @Test
    void testCreatePlan_nullDuration_throwsException() {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setName(PlanType.PREMIUM);
        plan.setPrice(10.0);
        plan.setDurationDays(null);
        plan.setDescription("Premium plan");

        assertThrows(IllegalArgumentException.class, () -> subscriptionPlanService.create(plan));
    }

    @Test
    void testCreatePlan_invalidDuration_throwsException() {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setName(PlanType.PREMIUM);
        plan.setPrice(10.0);
        plan.setDurationDays(0);
        plan.setDescription("Premium plan");

        assertThrows(IllegalArgumentException.class, () -> subscriptionPlanService.create(plan));
    }

    @Test
    void testCreatePlan_emptyDescription_throwsException() {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setName(PlanType.STANDARD);
        plan.setPrice(10.0);
        plan.setDurationDays(30);
        plan.setDescription("");

        assertThrows(IllegalArgumentException.class, () -> subscriptionPlanService.create(plan));
    }

    @Test
    void testGetAll_returnsList() {
        SubscriptionPlan plan1 = new SubscriptionPlan();
        plan1.setName(PlanType.FREEMIUM);
        SubscriptionPlan plan2 = new SubscriptionPlan();
        plan2.setName(PlanType.PREMIUM);

        when(subscriptionPlanRepository.findAll()).thenReturn(Arrays.asList(plan1, plan2));

        List<SubscriptionPlan> result = subscriptionPlanService.getAll();

        assertEquals(2, result.size());
        verify(subscriptionPlanRepository, times(1)).findAll();
    }

    @Test
    void testGetById_found() {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setName(PlanType.STANDARD);

        when(subscriptionPlanRepository.findById(1L)).thenReturn(Optional.of(plan));

        SubscriptionPlan result = subscriptionPlanService.getById(1L);

        assertNotNull(result);
        assertEquals(PlanType.STANDARD, result.getName());
    }

    @Test
    void testGetById_notFound_throwsException() {
        when(subscriptionPlanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> subscriptionPlanService.getById(99L));
    }

    @Test
    void testDelete_success() {
        when(subscriptionPlanRepository.existsById(1L)).thenReturn(true);
        doNothing().when(subscriptionPlanRepository).deleteById(1L);

        assertDoesNotThrow(() -> subscriptionPlanService.delete(1L));
        verify(subscriptionPlanRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDelete_notFound_throwsException() {
        when(subscriptionPlanRepository.existsById(99L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> subscriptionPlanService.delete(99L));
    }
}