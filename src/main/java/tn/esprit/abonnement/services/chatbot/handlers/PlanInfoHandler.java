package tn.esprit.abonnement.services.chatbot.handlers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tn.esprit.abonnement.services.chatbot.ChatbotDataFetcher;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PlanInfoHandler {

    private final ChatbotDataFetcher dataFetcher;

    public String handlePlanInfo() {
        Map<String, Object> plans = dataFetcher.getAvailablePlans();

        StringBuilder response = new StringBuilder();
        response.append("Here are our available subscription plans:\n\n");

        if (plans.containsKey("FREEMIUM")) {
            Map<String, Object> plan = (Map<String, Object>) plans.get("FREEMIUM");
            response.append("🎁 **FREEMIUM** - FREE\n");
            response.append("  ").append(plan.get("description")).append("\n\n");
        }

        if (plans.containsKey("STANDARD")) {
            Map<String, Object> plan = (Map<String, Object>) plans.get("STANDARD");
            response.append("⭐ **STANDARD** - $").append(plan.get("price")).append("/month\n");
            response.append("  ").append(plan.get("description")).append("\n\n");
        }

        if (plans.containsKey("PREMIUM")) {
            Map<String, Object> plan = (Map<String, Object>) plans.get("PREMIUM");
            response.append("🚀 **PREMIUM** - $").append(plan.get("price")).append("/month\n");
            response.append("  ").append(plan.get("description")).append("\n\n");
        }

        response.append("Which plan would you like to know more about?");

        return response.toString();
    }

    public String handlePlanComparison() {
        Map<String, Object> plans = dataFetcher.getAvailablePlans();

        StringBuilder response = new StringBuilder();
        response.append("Here's a quick comparison of our plans:\n\n");

        if (plans.containsKey("FREEMIUM")) {
            Map<String, Object> plan = (Map<String, Object>) plans.get("FREEMIUM");
            response.append("🎁 **FREEMIUM** (FREE)\n");
            response.append("  Perfect for getting started\n\n");
        }

        if (plans.containsKey("STANDARD")) {
            Map<String, Object> plan = (Map<String, Object>) plans.get("STANDARD");
            response.append("⭐ **STANDARD** ($").append(plan.get("price")).append("/month)\n");
            response.append("  Great for regular users\n\n");
        }

        if (plans.containsKey("PREMIUM")) {
            Map<String, Object> plan = (Map<String, Object>) plans.get("PREMIUM");
            response.append("🚀 **PREMIUM** ($").append(plan.get("price")).append("/month)\n");
            response.append("  For power users and businesses\n\n");
        }

        return response.toString();
    }
}