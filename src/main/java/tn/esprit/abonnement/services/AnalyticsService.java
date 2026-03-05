package tn.esprit.abonnement.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.abonnement.dto.AnalyticsDashboardDTO;
import tn.esprit.abonnement.dto.MonthlyGrowthDTO;
import tn.esprit.abonnement.dto.MonthlyRevenueDTO;
import tn.esprit.abonnement.entity.SubscriptionStatus;
import tn.esprit.abonnement.repository.UserSubscriptionRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UserSubscriptionRepository userSubscriptionRepository;

    public AnalyticsDashboardDTO getAdminDashboard() {
        long total = userSubscriptionRepository.countTotalSubscribers();
        long active = userSubscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE.name());
        long expired = userSubscriptionRepository.countByStatus(SubscriptionStatus.EXPIRED.name());
        long cancelled = userSubscriptionRepository.countByStatus(SubscriptionStatus.CANCELLED.name());

        List<MonthlyRevenueDTO> monthlyRevenue = userSubscriptionRepository.getMonthlyRevenue()
                .stream()
                .map(row -> new MonthlyRevenueDTO(
                        ((Number) row[0]).intValue(),
                        ((Number) row[1]).intValue(),
                        ((Number) row[2]).doubleValue()))
                .collect(Collectors.toList());

        List<MonthlyGrowthDTO> monthlyGrowth = userSubscriptionRepository.getMonthlyGrowth()
                .stream()
                .map(row -> new MonthlyGrowthDTO(
                        ((Number) row[0]).intValue(),
                        ((Number) row[1]).intValue(),
                        ((Number) row[2]).longValue()))
                .collect(Collectors.toList());

        double growthRate = computeGrowthRate(monthlyGrowth);

        return new AnalyticsDashboardDTO(
                total, active, expired, cancelled,
                monthlyRevenue, monthlyGrowth, growthRate);
    }

    private double computeGrowthRate(List<MonthlyGrowthDTO> monthlyGrowth) {
        if (monthlyGrowth.size() < 2) {
            return 0.0;
        }
        long current = monthlyGrowth.get(monthlyGrowth.size() - 1).getNewSubscribers();
        long previous = monthlyGrowth.get(monthlyGrowth.size() - 2).getNewSubscribers();
        if (previous == 0) {
            return 0.0;
        }
        return Math.round(((double) (current - previous) / previous) * 1000.0) / 10.0;
    }
}
