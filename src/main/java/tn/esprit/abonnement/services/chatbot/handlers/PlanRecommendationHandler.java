package tn.esprit.abonnement.services.chatbot.handlers;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import tn.esprit.abonnement.dto.SubscriptionSummary;
import tn.esprit.abonnement.services.chatbot.ChatbotDataFetcher;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@ConditionalOnBean(name = "openAiService")
public class PlanRecommendationHandler {

    private final ChatbotDataFetcher dataFetcher;

    public Map<String, Object> handlePlanRecommendation(Long userId) {
        SubscriptionSummary summary = dataFetcher.getUserSubscriptionSummary(userId);
        boolean closeToLimit = dataFetcher.isUserCloseToLimit(userId);
        String paymentStatus = dataFetcher.getPaymentStatus(userId);

        Map<String, Object> result = new HashMap<>();
        String response;
        String recommendedPlan = null;

        String currentPlan = summary.getCurrentPlan();

        if (currentPlan.equals("NONE") || currentPlan.equals("FREEMIUM")) {
            response = "You're currently on the Frequent plan. To get more features, consider upgrading!\n\n" +
                    "👉 **STANDARD** - Perfect for regular use\n" +
                    "👉 **PREMIUM** - For maximum features and support\n\n" +
                    "Would you like me to help you upgrade?";
            recommendedPlan = "STANDARD";
        } else if (currentPlan.equals("STANDARD")) {
            if (closeToLimit) {
                response = "You are close to your monthly limit.\n\n" +
                        "👉 The **PREMIUM** plan would be a better fit for your usage.\n\n" +
                        "Upgrade now to get unlimited access and priority support!";
                recommendedPlan = "PREMIUM";
            } else {
                response = "You're on the STANDARD plan, which is great for your current usage!\n\n" +
                        "If you need more features or anticipate higher usage, consider upgrading to **PREMIUM**.\n\n" +
                        "Otherwise, you're all set!";
            }
        } else if (currentPlan.equals("PREMIUM")) {
            response = "You're already on our **PREMIUM** plan - you have access to all features!\n\n" +
                    "Is there anything specific I can help you with?";
        } else {
            response = "Based on your current subscription, I recommend staying on your plan.\n\n" +
                    "If you have specific needs, let me know and I can suggest the best option!";
        }

        result.put("response", response);
        result.put("recommendedPlan", recommendedPlan);
        return result;
    }
}