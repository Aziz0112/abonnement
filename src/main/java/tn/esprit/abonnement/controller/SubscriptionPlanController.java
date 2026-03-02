package tn.esprit.abonnement.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.abonnement.entity.SubscriptionPlan;
import tn.esprit.abonnement.services.SubscriptionPlanService;

import java.util.List;

@RestController
@RequestMapping("/api/abonnements")
@RequiredArgsConstructor
public class SubscriptionPlanController {

    private final SubscriptionPlanService subscriptionPlanService;

    @PostMapping("/create-abonnement")
    public ResponseEntity<SubscriptionPlan> create(@RequestBody SubscriptionPlan plan) {
        SubscriptionPlan created = subscriptionPlanService.create(plan);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/get-all-abonnements")
    public ResponseEntity<List<SubscriptionPlan>> getAll() {
        return ResponseEntity.ok(subscriptionPlanService.getAll());
    }

    @GetMapping("/get-abonnement/{id}")
    public ResponseEntity<SubscriptionPlan> getById(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionPlanService.getById(id));
    }

    @PutMapping("/update-abonnement/{id}")
    public ResponseEntity<SubscriptionPlan> update(@PathVariable Long id,
                                                   @RequestBody SubscriptionPlan plan) {
        return ResponseEntity.ok(subscriptionPlanService.update(id, plan));
    }

    @DeleteMapping("/delete-abonnement/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subscriptionPlanService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
