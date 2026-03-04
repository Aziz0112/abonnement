package tn.esprit.abonnement.controller;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.abonnement.dto.CheckoutRequest;
import tn.esprit.abonnement.dto.CheckoutResponse;
import tn.esprit.abonnement.dto.ConfirmRequest;
import tn.esprit.abonnement.entity.SubscriptionPlan;
import tn.esprit.abonnement.entity.SubscriptionStatus;
import tn.esprit.abonnement.entity.UserSubscription;
import tn.esprit.abonnement.services.EmailService;
import tn.esprit.abonnement.services.InvoiceService;
import tn.esprit.abonnement.services.PdfService;
import tn.esprit.abonnement.services.SubscriptionPlanService;
import tn.esprit.abonnement.services.UserSubscriptionService;

import java.util.Map;

/**
 * Controller for Stripe Checkout payment flow
 */
@RestController
@RequestMapping("/api/abonnements")
public class StripePaymentController {

    private static final Logger logger = LoggerFactory.getLogger(StripePaymentController.class);

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Value("${stripe.public.key}")
    private String stripePublicKey;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Autowired
    private UserSubscriptionService subscriptionService;

    @Autowired
    private SubscriptionPlanService planService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private InvoiceService invoiceService;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
        logger.info("Stripe API initialized");
    }

    /**
     * Create a Stripe Checkout Session
     *
     * POST /api/payments/create-checkout-session
     */
    @PostMapping("/payments/create-checkout-session")
    public ResponseEntity<?> createCheckoutSession(@Valid @RequestBody CheckoutRequest request) {
        logger.info("Creating Stripe Checkout session for userId: {}, planId: {}", request.getUserId(),
                request.getPlanId());

        try {
            // Validate plan exists
            SubscriptionPlan plan = planService.getById(request.getPlanId());

            // Check for existing active subscription
            var existingSubscription = subscriptionService.getActiveSubscriptionByUserId(request.getUserId());
            if (existingSubscription.isPresent()
                    && existingSubscription.get().getStatus() == SubscriptionStatus.ACTIVE) {
                logger.warn("User {} already has an active subscription", request.getUserId());
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "You already have an active subscription"));
            }

            // Convert price to cents for Stripe (Stripe uses smallest currency unit)
            long priceInCents = Math.round(plan.getPrice() * 100);

            // Build the Checkout Session
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setCustomerEmail(request.getEmail())
                    .setSuccessUrl(frontendUrl + "/subscriptions/payment-success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(frontendUrl + "/subscriptions/payment-cancel")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("usd")
                                                    .setUnitAmount(priceInCents)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(
                                                                            plan.getName().name() + " Plan - MiNoLingo")
                                                                    .setDescription(plan.getDescription())
                                                                    .build())
                                                    .build())
                                    .build())
                    .putMetadata("userId", request.getUserId().toString())
                    .putMetadata("planId", request.getPlanId().toString())
                    .putMetadata("email", request.getEmail())
                    .build();

            Session session = Session.create(params);

            logger.info("Stripe Checkout session created: {}", session.getId());

            return ResponseEntity.ok(new CheckoutResponse(session.getId(), session.getUrl()));

        } catch (StripeException e) {
            logger.error("Stripe error creating checkout session: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create payment session: " + e.getMessage()));

        } catch (Exception e) {
            logger.error("Error creating checkout session: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }

    /**
     * Confirm payment after Stripe Checkout redirect
     *
     * POST /api/payments/confirm
     */
    @PostMapping("/payments/confirm")
    public ResponseEntity<?> confirmPayment(@Valid @RequestBody ConfirmRequest request) {
        logger.info("Confirming payment for session: {}", request.getSessionId());

        try {
            // Retrieve the Stripe session to verify payment
            Session session = Session.retrieve(request.getSessionId());

            if (!"paid".equals(session.getPaymentStatus())) {
                logger.warn("Payment not completed for session: {}", request.getSessionId());
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Payment has not been completed"));
            }

            // Check for existing active subscription (idempotency)
            var existingSubscription = subscriptionService.getActiveSubscriptionByUserId(request.getUserId());
            if (existingSubscription.isPresent()
                    && existingSubscription.get().getStatus() == SubscriptionStatus.ACTIVE) {
                logger.info("Subscription already active for user {} — returning existing", request.getUserId());
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Subscription is already active",
                        "subscription", existingSubscription.get()));
            }

            // Book the subscription
            UserSubscription subscription = subscriptionService.bookSubscription(
                    request.getUserId(),
                    request.getPlanId());

            logger.info("Subscription created with ID: {}", subscription.getId());

            // Feature 4: Store Stripe customer ID if available
            try {
                String customerId = session.getCustomer();
                if (customerId != null) {
                    subscription.setStripeCustomerId(customerId);
                }
            } catch (Exception e) {
                logger.warn("Could not retrieve Stripe customer ID: {}", e.getMessage());
            }

            // Feature 3: Create invoice for the subscription
            try {
                invoiceService.createInvoiceForSubscription(subscription, request.getSessionId());
            } catch (Exception e) {
                logger.error("Failed to create invoice: {}", e.getMessage(), e);
            }

            // Send confirmation email with PDF receipt
            try {
                String userEmail = request.getEmail() != null ? request.getEmail() : session.getCustomerEmail();
                String userName = "User";

                logger.info("Generating PDF receipt for: {}", userEmail);
                byte[] pdfBytes = pdfService.generateReceipt(userName, userEmail, subscription);

                logger.info("Sending confirmation email to: {}", userEmail);
                boolean emailSent = emailService.sendSubscriptionConfirmation(userEmail, userName, subscription,
                        pdfBytes);

                if (emailSent) {
                    logger.info("Confirmation email sent successfully to: {}", userEmail);
                } else {
                    logger.error("Failed to send confirmation email to: {}", userEmail);
                }
            } catch (Exception e) {
                logger.error("Error sending confirmation email: {}", e.getMessage(), e);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment confirmed — subscription activated!",
                    "subscription", subscription));

        } catch (StripeException e) {
            logger.error("Stripe error confirming payment: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to verify payment: " + e.getMessage()));

        } catch (Exception e) {
            logger.error("Error confirming payment: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "An error occurred: " + e.getMessage()));
        }
    }

    /**
     * Get the Stripe publishable key (for frontend initialization)
     *
     * GET /api/payments/config
     */
    @GetMapping("/payments/config")
    public ResponseEntity<?> getConfig() {
        return ResponseEntity.ok(Map.of("publishableKey", stripePublicKey));
    }

    /**
     * Health check endpoint
     *
     * GET /api/payments/health
     */
    @GetMapping("/payments/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "Stripe payment service is ready"));
    }
}