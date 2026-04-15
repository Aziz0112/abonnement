package tn.esprit.abonnement.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.abonnement.dto.BookRequest;
import tn.esprit.abonnement.dto.RecommendationDTO;
import tn.esprit.abonnement.entity.UserSubscription;
import tn.esprit.abonnement.services.RecommendationService;
import tn.esprit.abonnement.services.UserSubscriptionService;

import java.util.List;

@RestController
@RequestMapping("/api/abonnements")
@RequiredArgsConstructor
public class UserSubscriptionController {

    private final UserSubscriptionService userSubscriptionService;
    private final RecommendationService recommendationService;

    @PostMapping("/book")
    public ResponseEntity<UserSubscription> bookSubscription(@RequestBody BookRequest request) {
        UserSubscription subscription = userSubscriptionService.bookSubscription(request.getUserId(),
                request.getPlanId());
        return ResponseEntity.ok(subscription);
    }

    @GetMapping("/user/{userId}/current-subscription")
    public ResponseEntity<UserSubscription> getCurrentSubscription(@PathVariable Long userId) {
        return userSubscriptionService.getActiveSubscriptionByUserId(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/cancel-subscription/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelSubscription(@PathVariable Long id) {
        userSubscriptionService.cancelSubscription(id);
    }

    @PostMapping("/create-subscription")
    public ResponseEntity<UserSubscription> create(@RequestBody UserSubscription subscription) {
        UserSubscription created = userSubscriptionService.create(subscription);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/get-all-subscriptions")
    public ResponseEntity<List<UserSubscription>> getAll() {
        return ResponseEntity.ok(userSubscriptionService.getAll());
    }

    @GetMapping("/get-subscription/{id}")
    public ResponseEntity<UserSubscription> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userSubscriptionService.getById(id));
    }

    @PutMapping("/update-subscription/{id}")
    public ResponseEntity<UserSubscription> update(@PathVariable Long id,
            @RequestBody UserSubscription subscription) {
        return ResponseEntity.ok(userSubscriptionService.update(id, subscription));
    }

    @DeleteMapping("/delete-subscription/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userSubscriptionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}/recommendation")
    public ResponseEntity<RecommendationDTO> getRecommendation(@PathVariable Long userId) {
        return ResponseEntity.ok(recommendationService.getRecommendationForUser(userId));
    }

    @PatchMapping("/{id}/auto-renew")
    public ResponseEntity<UserSubscription> toggleAutoRenew(@PathVariable Long id,
            @RequestParam boolean enabled) {
        return ResponseEntity.ok(userSubscriptionService.toggleAutoRenew(id, enabled));
    }
}
