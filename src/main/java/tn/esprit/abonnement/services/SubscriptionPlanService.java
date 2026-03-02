package tn.esprit.abonnement.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.abonnement.entity.SubscriptionPlan;
import tn.esprit.abonnement.repository.SubscriptionPlanRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    @Transactional
    public SubscriptionPlan create(SubscriptionPlan plan) {
        // Validate price
        if (plan.getPrice() == null) {
            throw new IllegalArgumentException("Price is required");
        }
        if (plan.getPrice() < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }

        // Validate duration
        if (plan.getDurationDays() == null) {
            throw new IllegalArgumentException("Duration is required");
        }
        if (plan.getDurationDays() < 1) {
            throw new IllegalArgumentException("Duration must be at least 1 day");
        }

        // Validate description
        if (plan.getDescription() == null || plan.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required");
        }

        // Validate plan name
        if (plan.getName() == null) {
            throw new IllegalArgumentException("Plan name is required");
        }

        return subscriptionPlanRepository.save(plan);
    }

    @Transactional
    public SubscriptionPlan update(Long id, SubscriptionPlan plan) {
        SubscriptionPlan existing = subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SubscriptionPlan not found with id: " + id));

        // Validate price
        if (plan.getPrice() != null) {
            if (plan.getPrice() < 0) {
                throw new IllegalArgumentException("Price cannot be negative");
            }
            existing.setPrice(plan.getPrice());
        }

        // Validate duration
        if (plan.getDurationDays() != null) {
            if (plan.getDurationDays() < 1) {
                throw new IllegalArgumentException("Duration must be at least 1 day");
            }
            existing.setDurationDays(plan.getDurationDays());
        }

        // Update other fields
        if (plan.getName() != null) {
            existing.setName(plan.getName());
        }
        if (plan.getDescription() != null) {
            existing.setDescription(plan.getDescription());
        }

        return subscriptionPlanRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!subscriptionPlanRepository.existsById(id)) {
            throw new RuntimeException("SubscriptionPlan not found with id: " + id);
        }
        subscriptionPlanRepository.deleteById(id);
    }

    public SubscriptionPlan getById(Long id) {
        return subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SubscriptionPlan not found with id: " + id));
    }

    public List<SubscriptionPlan> getAll() {
        return subscriptionPlanRepository.findAll();
    }
}