package tn.esprit.abonnement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.abonnement.entity.SubscriptionStatus;
import tn.esprit.abonnement.entity.UserSubscription;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    // Find all subscriptions for a user
    List<UserSubscription> findByUserId(Long userId);

    // Find active subscriptions for a user
    @Query("SELECT s FROM UserSubscription s WHERE s.userId = :userId AND s.status = 'ACTIVE'")
    Optional<UserSubscription> findActiveSubscriptionByUserId(@Param("userId") Long userId);

    // Find subscriptions by status
    List<UserSubscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);

    // Find subscriptions by plan
    List<UserSubscription> findByPlanId(Long planId);

    // Find expired subscriptions (need to update status)
    @Query("SELECT s FROM UserSubscription s WHERE s.status = 'ACTIVE' AND s.expiresAt < :now")
    List<UserSubscription> findExpiredSubscriptions(@Param("now") LocalDateTime now);

    // Count active subscriptions for a plan
    long countByPlanIdAndStatus(Long planId, SubscriptionStatus status);
}
