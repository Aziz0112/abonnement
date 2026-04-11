package tn.esprit.abonnement.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.esprit.abonnement.dto.AnalyticsDashboardDTO;
import tn.esprit.abonnement.dto.ChatRequest;
import tn.esprit.abonnement.dto.ChatResponse;
import tn.esprit.abonnement.entity.UserSubscription;
import tn.esprit.abonnement.repository.UserSubscriptionRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsChatbotService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openrouter.api.key}")
    private String openRouterApiKey;

    @Value("${openrouter.api.url:https://openrouter.ai/api/v1/chat/completions}")
    private String openRouterApiUrl;

    @Value("${openrouter.model:openai/gpt-3.5-turbo}")
    private String model;

    private static final double TEMPERATURE = 0.7;
    private static final int MAX_TOKENS = 1000;

    /**
     * Main chat method that orchestrates the AI response
     */
    public ChatResponse chat(ChatRequest request) {
        try {
            // 1. Build system prompt with MinoLingo context
            String systemPrompt = buildSystemPrompt(request);

            // 2. Build messages for OpenRouter
            List<Map<String, String>> messages = buildMessages(systemPrompt, request);

            // 3. Call OpenRouter API
            String aiResponse = callOpenRouterAPI(messages);

            // 4. Build and return response
            return ChatResponse.builder()
                    .message(aiResponse)
                    .suggestions(generateSuggestions(request.getMessage()))
                    .build();

        } catch (Exception e) {
            log.error("Error in chat service", e);
            return ChatResponse.builder()
                    .message("I apologize, but I encountered an error. Please try again.")
                    .suggestions(new ArrayList<>())
                    .build();
        }
    }

    /**
     * Build dynamic system prompt with MinoLingo context and user information
     */
    private String buildSystemPrompt(ChatRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        // Platform context
        prompt.append("You are an AI analytics assistant for MinoLingo, ");
        prompt.append("an education platform for children to learn English and other courses.\n\n");

        // User context
        if (request.getUserId() != null) {
            UserSubscription userSub = userSubscriptionRepository
                    .findFirstByUserIdOrderByIdDesc(request.getUserId())
                    .orElse(null);
            
            if (userSub != null) {
                prompt.append("Current Admin Context:\n");
                prompt.append("- Platform Role: Admin\n");
                prompt.append("- Subscription System: Active\n");
                prompt.append("- Subscription Plans Available: Basic, Pro, Premium\n");
                if (userSub.getPlan() != null) {
                    prompt.append("- Managing: ").append(userSub.getPlan().getName()).append(" plans\n");
                }
                prompt.append("\n");
            }
        } else {
            prompt.append("Current Context: Admin viewing platform analytics\n\n");
        }

        // Analytics context
        if (request.getAnalytics() != null) {
            AnalyticsDashboardDTO analytics = request.getAnalytics();
            prompt.append("Current Analytics Data:\n");
            prompt.append("- Total Subscribers: ").append(analytics.getTotalSubscribers()).append("\n");
            prompt.append("- Active Subscribers: ").append(analytics.getActiveSubscribers()).append("\n");
            prompt.append("- Expired Subscriptions: ").append(analytics.getExpiredSubscribers()).append("\n");
            prompt.append("- Cancelled Subscriptions: ").append(analytics.getCancelledSubscribers()).append("\n");
            prompt.append("- Growth Rate: ").append(analytics.getGrowthRatePercent()).append("%\n");
            prompt.append("- Monthly Revenue Entries: ").append(analytics.getMonthlyRevenue().size()).append("\n");
            prompt.append("- Monthly Growth Entries: ").append(analytics.getMonthlyGrowth().size()).append("\n\n");
        }

        // Platform details
        prompt.append("Platform Details:\n");
        prompt.append("- Name: MinoLingo\n");
        prompt.append("- Purpose: Kids education platform for English and other courses\n");
        prompt.append("- Target Audience: Children learning languages\n");
        prompt.append("- Courses: English, Spanish, French, German\n");
        prompt.append("- Mission: Making language learning fun and engaging for kids\n\n");

        // Guidelines
        prompt.append("Guidelines for responses:\n");
        prompt.append("1. Be encouraging, educational, and professional\n");
        prompt.append("2. Use simple, kid-friendly language when appropriate\n");
        prompt.append("3. Focus on learning outcomes and educational value\n");
        prompt.append("4. Provide personalized recommendations based on analytics data\n");
        prompt.append("5. Be warm and approachable while being data-driven\n");
        prompt.append("6. Use specific numbers from the analytics data\n");
        prompt.append("7. Compare current metrics with previous periods when relevant\n");
        prompt.append("8. Provide actionable, data-driven recommendations\n");
        prompt.append("9. If asked about trends, analyze the monthly data provided\n");
        prompt.append("10. Be concise yet detailed - aim for 100-200 words per response\n");
        prompt.append("11. If unsure, ask clarifying questions\n");
        prompt.append("12. Always maintain context that this is for MinoLingo education platform\n");
        prompt.append("13. Answer ANY question about the data, platform, or education topics\n\n");

        prompt.append("Remember: You're helping manage and improve a kids' education platform. ");
        prompt.append("Your responses should reflect the importance of education and children's learning.");

        return prompt.toString();
    }

    /**
     * Build messages list for OpenRouter API
     */
    private List<Map<String, String>> buildMessages(String systemPrompt, ChatRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();

        // System message
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);

        // User message
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", request.getMessage());
        messages.add(userMsg);

        return messages;
    }

    /**
     * Call OpenRouter API with the provided messages
     */
    private String callOpenRouterAPI(List<Map<String, String>> messages) {
        try {
            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("temperature", TEMPERATURE);
            requestBody.put("max_tokens", MAX_TOKENS);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + openRouterApiKey);
            headers.set("HTTP-Referer", "https://minolingo.online");
            headers.set("X-Title", "MinoLingo Education Platform");

            // Create request entity
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Make API call
            ResponseEntity<String> response = restTemplate.exchange(
                    openRouterApiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            // Parse response
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).path("message");
                return message.path("content").asText();
            }

            return "I apologize, but I couldn't generate a response. Please try again.";

        } catch (Exception e) {
            log.error("Error calling OpenRouter API", e);
            return "I encountered an error connecting to the AI service. Please try again.";
        }
    }

    /**
     * Generate relevant suggestions based on the user's message
     */
    private List<String> generateSuggestions(String userMessage) {
        List<String> suggestions = new ArrayList<>();
        String lowerMessage = userMessage.toLowerCase();

        if (lowerMessage.contains("revenue") || lowerMessage.contains("money")) {
            suggestions.add("Show revenue breakdown by month");
            suggestions.add("Compare with previous month");
            suggestions.add("Revenue growth rate analysis");
        } else if (lowerMessage.contains("subscriber") || lowerMessage.contains("user")) {
            suggestions.add("Subscriber growth trend");
            suggestions.add("Active vs inactive analysis");
            suggestions.add("Churn rate insights");
        } else if (lowerMessage.contains("recommend") || lowerMessage.contains("improve") || lowerMessage.contains("strategy")) {
            suggestions.add("Increase engagement strategies");
            suggestions.add("Retention improvement tips");
            suggestions.add("Growth acceleration ideas");
        } else if (lowerMessage.contains("course") || lowerMessage.contains("lesson") || lowerMessage.contains("learning")) {
            suggestions.add("Most popular courses");
            suggestions.add("Learning completion rates");
            suggestions.add("Course performance analysis");
        } else if (lowerMessage.contains("growth") || lowerMessage.contains("trend")) {
            suggestions.add("Growth rate analysis");
            suggestions.add("Seasonal patterns");
            suggestions.add("Future projections");
        } else {
            suggestions.add("How much revenue this month?");
            suggestions.add("Subscriber growth analysis");
            suggestions.add("Course performance insights");
            suggestions.add("Recommend improvements");
        }

        return suggestions;
    }
}