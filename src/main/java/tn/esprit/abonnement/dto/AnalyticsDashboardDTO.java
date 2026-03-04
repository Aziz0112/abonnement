package tn.esprit.abonnement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalyticsDashboardDTO {
    private long totalSubscribers;
    private long activeSubscribers;
    private long expiredSubscribers;
    private long cancelledSubscribers;
    private List<MonthlyRevenueDTO> monthlyRevenue;
    private List<MonthlyGrowthDTO> monthlyGrowth;
    private double growthRatePercent;
}
