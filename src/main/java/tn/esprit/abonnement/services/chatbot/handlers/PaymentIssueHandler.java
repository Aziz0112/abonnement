package tn.esprit.abonnement.services.chatbot.handlers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tn.esprit.abonnement.services.chatbot.ChatbotDataFetcher;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentIssueHandler {

    private final ChatbotDataFetcher dataFetcher;

    public List<String> handlePaymentIssue(Long userId) {
        String paymentStatus = dataFetcher.getPaymentStatus(userId);
        List<String> result = new ArrayList<>();

        StringBuilder response = new StringBuilder();
        response.append("Your payment may have failed due to:\n\n");
        response.append("• Expired card\n");
        response.append("• Insufficient funds\n");
        response.append("• Bank blocking the transaction\n");
        response.append("• Incorrect card details\n\n");
        response.append("👉 Please check your card details or try another payment method.\n\n");

        response.append("Next steps:\n");
        response.append("1. Go to Settings → Billing\n");
        response.append("2. Update your payment method\n");
        response.append("3. Try the payment again\n\n");

        if (paymentStatus.contains("Payment failed")) {
            response.append("💡 Tip: Contact your bank if you're unsure why the transaction was declined.");
        }

        result.add(response.toString());
        result.add("Update Payment Method");
        result.add("View Billing History");
        return result;
    }

    public String handlePaymentMethods() {
        return "We accept the following payment methods:\n\n" +
                "💳 Credit/Debit Cards (Visa, MasterCard, American Express)\n" +
                "🏦 PayPal\n" +
                "📱 Apple Pay / Google Pay\n\n" +
                "To add or update a payment method:\n" +
                "Settings → Billing → Payment Methods";
    }
}