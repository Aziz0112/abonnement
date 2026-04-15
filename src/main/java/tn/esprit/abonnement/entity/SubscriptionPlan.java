package tn.esprit.abonnement.entity;

import jakarta.persistence.*;
import lombok.*;
@Table(name = "subscriptionplans")

@Entity
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "name")
    private PlanType name; // FREEMIUM, STANDARD, PREMIUM

    @Column(name = "price")
    private Double price;

    @Column(name = "duration")
    private Integer durationDays; // e.g. 30

    @Column(name = "description")
    private String description;
}
