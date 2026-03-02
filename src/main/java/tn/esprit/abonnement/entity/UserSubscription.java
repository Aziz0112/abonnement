package tn.esprit.abonnement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Table(name = "usersubscriptions")

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @ManyToOne
    @JoinColumn(name = "plan_id")
    private SubscriptionPlan plan;
    @Column(name = "subscribed")
    private LocalDateTime subscribedAt;
    @Column(name = "expires")
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SubscriptionStatus status;
}
