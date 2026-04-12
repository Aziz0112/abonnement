package tn.esprit.abonnement.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.esprit.abonnement.dto.AnalyticsDashboardDTO;
import tn.esprit.abonnement.dto.HealthScoreDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthScoreService {

    private final AnalyticsService analyticsService;
    private final RestTemplate restTemplate;

    @Value("${health.service.url:http://localhost:8086}")
    private String healthServiceUrl;

    @Value("${health.service.api-key:minolingo-health-secret-2026}")
    private String healthApiKey;

    public HealthScoreDTO computeHealthScore() {
        AnalyticsDashboardDTO dashboard = analyticsService.getAdminDashboard();

        Map<String, Object> payload = new HashMap<>();
        payload.put("totalSubscribers", dashboard.getTotalSubscribers());
        payload.put("activeSubscribers", dashboard.getActiveSubscribers());
        payload.put("expiredSubscribers", dashboard.getExpiredSubscribers());
        payload.put("cancelledSubscribers", dashboard.getCancelledSubscribers());
        payload.put("growthRatePercent", dashboard.getGrowthRatePercent());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Api-Key", healthApiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            return restTemplate.postForObject(
                    healthServiceUrl + "/health-score",
                    request,
                    HealthScoreDTO.class);
        } catch (Exception e) {
            log.error("Health score service unreachable: {}", e.getMessage());
            return new HealthScoreDTO(
                    null,
                    "N/A",
                    "Service unavailable",
                    "gray",
                    List.of("Health score service is offline"),
                    new HashMap<>());
        }
    }
}
