package tn.esprit.abonnement.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.esprit.abonnement.dto.ChatRequest;
import tn.esprit.abonnement.dto.ChatResponse;

import java.util.ArrayList;

@Service
@Slf4j
public class AnalyticsChatbotService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${chatbot.service.url:http://localhost:8085}")
    private String chatbotServiceUrl;

    @Value("${chatbot.service.api-key:minolingo-chatbot-secret-2026}")
    private String chatbotApiKey;

    public ChatResponse chat(ChatRequest request) {
        log.info("Forwarding chat request to Python chatbot service. userId={}", request.getUserId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Api-Key", chatbotApiKey);

        HttpEntity<ChatRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<ChatResponse> response = restTemplate.exchange(
                    chatbotServiceUrl + "/chat",
                    HttpMethod.POST,
                    entity,
                    ChatResponse.class
            );

            log.info("Python chatbot responded successfully");
            return response.getBody();

        } catch (Exception e) {
            log.error("Python chatbot service error: {}", e.getMessage());
            return ChatResponse.builder()
                    .message("I apologize, but the AI service is unavailable. Please try again.")
                    .suggestions(new ArrayList<>())
                    .build();
        }
    }
}
