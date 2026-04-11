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

    // Find the most recent subscription for a user (used for chatbot personalization)
    Optional<UserSubscription> findFirstByUserIdOrderByIdDesc(Long userId);

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

    // Feature 1: Analytics queries (PostgreSQL-compatible using native SQL)
    @Query(value = "SELECT COUNT(*) FROM usersubscriptions", nativeQuery = true)
    long countTotalSubscribers();

    @Query(value = "SELECT COUNT(*) FROM usersubscriptions WHERE status = :status", nativeQuery = true)
    long countByStatus(@Param("status") String status);

    @Query(value = "SELECT EXTRACT(YEAR FROM s.subscribed) AS yr, " +
           "EXTRACT(MONTH FROM s.subscribed) AS mo, " +
           "SUM(sp.price) AS revenue " +
           "FROM usersubscriptions s " +
           "JOIN subscriptionplans sp ON s.plan_id = sp.id " +
           "WHERE s.status <> 'CANCELLED' " +
           "GROUP BY EXTRACT(YEAR FROM s.subscribed), EXTRACT(MONTH FROM s.subscribed) " +
           "ORDER BY yr ASC, mo ASC", nativeQuery = true)
    List<Object[]> getMonthlyRevenue();

    @Query(value = "SELECT EXTRACT(YEAR FROM s.subscribed) AS yr, " +
           "EXTRACT(MONTH FROM s.subscribed) AS mo, " +
           "COUNT(*) AS newSubs " +
           "FROM usersubscriptions s " +
           "GROUP BY EXTRACT(YEAR FROM s.subscribed), EXTRACT(MONTH FROM s.subscribed) " +
           "ORDER BY yr ASC, mo ASC", nativeQuery = true)
    List<Object[]> getMonthlyGrowth();

    // Feature 2: Expiry reminder query
    @Query("SELECT s FROM UserSubscription s " +
           "WHERE s.status = 'ACTIVE' " +
           "AND s.expiresAt BETWEEN :now AND :deadline " +
           "AND s.reminderSent = false")
    List<UserSubscription> findSubscriptionsExpiringBetween(
        @Param("now") LocalDateTime now,
        @Param("deadline") LocalDateTime deadline);
}
