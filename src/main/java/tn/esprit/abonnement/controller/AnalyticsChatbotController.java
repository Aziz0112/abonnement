package tn.esprit.abonnement.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.abonnement.dto.ChatRequest;
import tn.esprit.abonnement.dto.ChatResponse;
import tn.esprit.abonnement.services.AnalyticsChatbotService;

@RestController
@RequestMapping("/api/abonnements/analytics/chat")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsChatbotController {

    private final AnalyticsChatbotService chatbotService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        log.info("Received chat request from user ID: {}", request.getUserId());
        log.info("User message: {}", request.getMessage());
        
        try {
            ChatResponse response = chatbotService.chat(request);
            log.info("Generated AI response successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing chat request", e);
            ChatResponse errorResponse = ChatResponse.builder()
                    .message("I apologize, but I encountered an error. Please try again.")
                    .suggestions(new java.util.ArrayList<>())
                    .build();
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}