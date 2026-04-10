package tn.esprit.abonnement.services.chatbot;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import tn.esprit.abonnement.config.OpenAIConfig;
import tn.esprit.abonnement.dto.ChatbotRequest;
import tn.esprit.abonnement.dto.ChatbotResponse;
import tn.esprit.abonnement.dto.SubscriptionSummary;
import tn.esprit.abonnement.services.chatbot.handlers.PaymentIssueHandler;
import tn.esprit.abonnement.services.chatbot.handlers.PlanInfoHandler;
import tn.esprit.abonnement.services.chatbot.handlers.PlanRecommendationHandler;
import tn.esprit.abonnement.services.chatbot.handlers.SubscriptionManagementHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(OpenAiService.class)
public class ChatbotService {

    private final OpenAiService openAiService;
    private final OpenAIConfig openAIConfig;
    private final ChatbotDataFetcher dataFetcher;
    private final PlanInfoHandler planInfoHandler;
    private final PlanRecommendationHandler planRecommendationHandler;
    private final PaymentIssueHandler paymentIssueHandler;
    private final SubscriptionManagementHandler subscriptionManagementHandler;

    private static final String SYSTEM_PROMPT = """
            You are a helpful subscription support chatbot. Your role is to:
            1. Help users understand subscription plans
            2. Recommend the best plan based on usage
            3. Solve payment issues
            4. Guide users to upgrade or manage their subscription
            
            BEHAVIOR RULES:
            - Always be short and clear
            - Be helpful and friendly
            - Avoid technical jargon
            - Personalize responses using user data
            - If unsure, ask a clarifying question
            
            When classifying user intent, respond with one of these categories:
            - PLAN_INFO: User asks about plans, pricing, features
            - PLAN_RECOMMENDATION: User asks which plan to choose
            - PAYMENT_ISSUE: User mentions payment failure, billing problems
            - UPGRADE: User asks how to upgrade
            - DOWNGRADE: User asks how to downgrade
            - CANCEL: User wants to cancel
            - MANAGE_SUBSCRIPTION: User wants to view or manage their subscription
            - GENERAL: General questions or greetings
            
            Respond in JSON format with:
            {
              "intent": "category",
              "confidence": 0.0-1.0
            }
            """;

    public ChatbotResponse handleMessage(ChatbotRequest request) {
        log.info("Processing chatbot message for user: {}", request.getUserId());

        // Get user context
        SubscriptionSummary summary = dataFetcher.getUserSubscriptionSummary(request.getUserId());

        // Classify intent using OpenAI
        String intent = classifyIntent(request.getMessage());

        // Route to appropriate handler
        return routeToHandler(intent, request.getUserId(), request.getMessage(), summary);
    }

    private String classifyIntent(String message) {
        try {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), SYSTEM_PROMPT));
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), message));

            ChatCompletionRequest chatRequest = ChatCompletionRequest.builder()
                    .model(openAIConfig.getModel())
                    .messages(messages)
                    .temperature(0.3)
                    .maxTokens(100)
                    .build();

            String response = openAiService.createChatCompletion(chatRequest)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();

            // Extract intent from JSON response
            if (response.contains("PLAN_INFO")) return "PLAN_INFO";
            if (response.contains("PLAN_RECOMMENDATION")) return "PLAN_RECOMMENDATION";
            if (response.contains("PAYMENT_ISSUE")) return "PAYMENT_ISSUE";
            if (response.contains("UPGRADE")) return "UPGRADE";
            if (response.contains("DOWNGRADE")) return "DOWNGRADE";
            if (response.contains("CANCEL")) return "CANCEL";
            if (response.contains("MANAGE_SUBSCRIPTION")) return "MANAGE_SUBSCRIPTION";

            return "GENERAL";

        } catch (Exception e) {
            log.error("Error classifying intent: {}", e.getMessage());
            return "GENERAL";
        }
    }

    private ChatbotResponse routeToHandler(String intent, Long userId, String message, SubscriptionSummary summary) {
        ChatbotResponse.ChatbotResponseBuilder responseBuilder = ChatbotResponse.builder();

        switch (intent) {
            case "PLAN_INFO":
                String planInfo = planInfoHandler.handlePlanInfo();
                responseBuilder.response(planInfo);
                responseBuilder.intent("PLAN_INFO");
                responseBuilder.suggestedActions(Arrays.asList("View Plans", "Compare Plans", "Get Recommendation"));
                break;

            case "PLAN_RECOMMENDATION":
                var recommendation = planRecommendationHandler.handlePlanRecommendation(userId);
                responseBuilder.response((String) recommendation.get("response"));
                responseBuilder.recommendedPlan((String) recommendation.get("recommendedPlan"));
                responseBuilder.intent("PLAN_RECOMMENDATION");
                responseBuilder.suggestedActions(Arrays.asList("Upgrade Now", "View Plans", "Learn More"));
                break;

            case "PAYMENT_ISSUE":
                var paymentResult = paymentIssueHandler.handlePaymentIssue(userId);
                responseBuilder.response(paymentResult.get(0));
                responseBuilder.intent("PAYMENT_ISSUE");
                List<String> paymentActions = new ArrayList<>(paymentResult.subList(1, paymentResult.size()));
                responseBuilder.suggestedActions(paymentActions);
                break;

            case "UPGRADE":
                var upgradeResult = subscriptionManagementHandler.handleUpgrade();
                responseBuilder.response(upgradeResult.get(0));
                responseBuilder.intent("UPGRADE");
                List<String> upgradeActions = new ArrayList<>(upgradeResult.subList(1, upgradeResult.size()));
                responseBuilder.suggestedActions(upgradeActions);
                break;

            case "DOWNGRADE":
                var downgradeResult = subscriptionManagementHandler.handleDowngrade();
                responseBuilder.response(downgradeResult.get(0));
                responseBuilder.intent("DOWNGRADE");
                List<String> downgradeActions = new ArrayList<>(downgradeResult.subList(1, downgradeResult.size()));
                responseBuilder.suggestedActions(downgradeActions);
                break;

            case "CANCEL":
                var cancelResult = subscriptionManagementHandler.handleCancel();
                responseBuilder.response(cancelResult.get(0));
                responseBuilder.intent("CANCEL");
                List<String> cancelActions = new ArrayList<>(cancelResult.subList(1, cancelResult.size()));
                responseBuilder.suggestedActions(cancelActions);
                break;

            case "MANAGE_SUBSCRIPTION":
                var manageResult = subscriptionManagementHandler.handleManageSubscription(userId);
                responseBuilder.response(manageResult.get(0));
                responseBuilder.intent("MANAGE_SUBSCRIPTION");
                List<String> manageActions = new ArrayList<>(manageResult.subList(1, manageResult.size()));
                responseBuilder.suggestedActions(manageActions);
                break;

            case "GENERAL":
            default:
                responseBuilder.response(generateGeneralResponse(message, summary));
                responseBuilder.intent("GENERAL");
                responseBuilder.suggestedActions(Arrays.asList("View Plans", "Manage Subscription", "Get Help"));
                break;
        }

        return responseBuilder.build();
    }

    private String generateGeneralResponse(String message, SubscriptionSummary summary) {
        // Use OpenAI to generate a general response with context
        String context = String.format(
                "User is on %s plan with status %s. ",
                summary.getCurrentPlan(),
                summary.getStatus()
        );

        String prompt = context + "User says: \"" + message + "\". Provide a helpful, friendly response about subscription-related topics. Keep it short and clear.";

        try {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                    "You are a helpful subscription support chatbot. Be short, clear, friendly, and avoid technical jargon."));
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), prompt));

            ChatCompletionRequest chatRequest = ChatCompletionRequest.builder()
                    .model(openAIConfig.getModel())
                    .messages(messages)
                    .temperature(0.7)
                    .maxTokens(200)
                    .build();

            return openAiService.createChatCompletion(chatRequest)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();

        } catch (Exception e) {
            log.error("Error generating general response: {}", e.getMessage());
            return "Hi! I'm here to help you with your subscription. You can ask me about plans, payments, or how to manage your account. What would you like to know?";
        }
    }
}