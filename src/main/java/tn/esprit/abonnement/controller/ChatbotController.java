package tn.esprit.abonnement.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.abonnement.dto.ChatbotRequest;
import tn.esprit.abonnement.dto.ChatbotResponse;
import tn.esprit.abonnement.services.chatbot.ChatbotService;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(ChatbotService.class)
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/message")
    public ResponseEntity<ChatbotResponse> sendMessage(@RequestBody ChatbotRequest request) {
        log.info("Received chatbot message from user: {}", request.getUserId());

        if (request.getUserId() == null) {
            log.warn("Chatbot request missing userId");
            return ResponseEntity.badRequest().build();
        }

        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            log.warn("Chatbot request missing message");
            return ResponseEntity.badRequest().build();
        }

        try {
            ChatbotResponse response = chatbotService.handleMessage(request);
            log.info("Chatbot response generated successfully for user: {}", request.getUserId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing chatbot message for user {}: {}", request.getUserId(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Chatbot service is running");
    }
}