package tn.esprit.abonnement.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.abonnement.dto.AnalyticsDashboardDTO;
import tn.esprit.abonnement.dto.HealthScoreDTO;
import tn.esprit.abonnement.services.AnalyticsService;
import tn.esprit.abonnement.services.HealthScoreService;

@RestController
@RequestMapping("/api/abonnements/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final HealthScoreService healthScoreService;

    @GetMapping("/dashboard")
    public ResponseEntity<AnalyticsDashboardDTO> getDashboard() {
        return ResponseEntity.ok(analyticsService.getAdminDashboard());
    }

    @GetMapping("/health-score")
    public ResponseEntity<HealthScoreDTO> getHealthScore() {
        return ResponseEntity.ok(healthScoreService.computeHealthScore());
    }
}
