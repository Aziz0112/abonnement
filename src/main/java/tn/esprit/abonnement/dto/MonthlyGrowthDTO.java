package tn.esprit.abonnement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyGrowthDTO {
    private int year;
    private int month;
    private long newSubscribers;
}
