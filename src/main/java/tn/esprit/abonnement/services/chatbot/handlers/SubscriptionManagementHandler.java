package tn.esprit.abonnement.services.chatbot.handlers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tn.esprit.abonnement.dto.SubscriptionSummary;
import tn.esprit.abonnement.services.chatbot.ChatbotDataFetcher;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SubscriptionManagementHandler {

    private final ChatbotDataFetcher dataFetcher;

    public List<String> handleUpgrade() {
        List<String> result = new ArrayList<>();

        String response = "You can upgrade your plan from your dashboard:\n\n" +
                "👉 Go to **Settings → Subscription → Upgrade Plan**\n\n" +
                "The change will be applied instantly.\n\n" +
                "Your new features will be available immediately after the upgrade!";

        result.add(response);
        result.add("View Plans");
        result.add("Upgrade Now");
        return result;
    }

    public List<String> handleDowngrade() {
        List<String> result = new ArrayList<>();

        String response = "To downgrade your plan:\n\n" +
                "👉 Go to **Settings → Subscription → Change Plan**\n\n" +
                "Important notes:\n" +
                "• The downgrade takes effect at the end of your current billing period\n" +
                "• You'll keep your current features until then\n" +
                "• Any unused premium features will be unavailable after the downgrade\n\n" +
                "Are you sure you want to downgrade?";

        result.add(response);
        result.add("Confirm Downgrade");
        result.add("Cancel");
        return result;
    }

    public List<String> handleManageSubscription(Long userId) {
        SubscriptionSummary summary = dataFetcher.getUserSubscriptionSummary(userId);
        List<String> result = new ArrayList<>();

        StringBuilder response = new StringBuilder();
        response.append("Here's your current subscription:\n\n");
        response.append("📊 **Plan**: ").append(summary.getCurrentPlan()).append("\n");
        response.append("💰 **Price**: $").append(summary.getMonthlyPrice()).append("/month\n");
        response.append("📅 **Status**: ").append(summary.getStatus()).append("\n");
        response.append("🔄 **Auto-renew**: ").append(summary.isAutoRenew() ? "Yes" : "No").append("\n\n");

        if (summary.getExpiresAt() != null) {
            response.append("⏰ **Expires**: ").append(summary.getExpiresAt()).append("\n\n");
        }

        response.append("What would you like to do?\n");
        response.append("• Upgrade plan\n");
        response.append("• Downgrade plan\n");
        response.append("• Cancel subscription\n");
        response.append("• Manage payment method");

        result.add(response.toString());
        result.add("Upgrade Plan");
        result.add("Cancel Subscription");
        result.add("View Invoices");
        return result;
    }

    public List<String> handleCancel() {
        List<String> result = new ArrayList<>();

        String response = "To cancel your subscription:\n\n" +
                "👉 Go to **Settings → Subscription → Cancel Plan**\n\n" +
                "Important information:\n" +
                "• Access continues until the end of your current billing period\n" +
                "• You can reactivate anytime\n" +
                "• Your data will be preserved for 30 days after cancellation\n\n" +
                "We're sorry to see you go! Is there anything we can do to help?";

        result.add(response);
        result.add("Cancel Subscription");
        result.add("Keep Subscription");
        return result;
    }
}