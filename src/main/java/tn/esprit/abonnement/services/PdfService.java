package tn.esprit.abonnement.services;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.esprit.abonnement.entity.SubscriptionPlan;
import tn.esprit.abonnement.entity.UserSubscription;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Service for generating PDF receipts
 * Creates professional payment receipts with subscription details
 */
@Service
public class PdfService {

    private static final Logger logger = LoggerFactory.getLogger(PdfService.class);
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter SIMPLE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Value("${app.platform.name}")
    private String platformName;

    @Value("${app.support.email}")
    private String supportEmail;

    /**
     * Generate a PDF receipt for a subscription payment
     * 
     * @param user User information (name, email)
     * @param subscription Subscription details
     * @return PDF as byte array
     */
    public byte[] generateReceipt(String userName, String userEmail, UserSubscription subscription) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Fonts
            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // Generate unique receipt ID and transaction ID
            String receiptId = "RCT-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String transactionId = UUID.randomUUID().toString();

            // HEADER SECTION
            addHeader(document, boldFont, normalFont, receiptId);

            // CUSTOMER INFO SECTION
            addCustomerInfo(document, boldFont, normalFont, userName, userEmail, subscription.getUserId());

            // SUBSCRIPTION DETAILS SECTION
            addSubscriptionDetails(document, boldFont, normalFont, subscription);

            // PAYMENT DETAILS SECTION
            addPaymentDetails(document, boldFont, normalFont, subscription.getPlan(), transactionId);

            // FOOTER SECTION
            addFooter(document, normalFont);

            document.close();

            logger.info("PDF receipt generated successfully. Receipt ID: {}", receiptId);
            return baos.toByteArray();

        } catch (Exception e) {
            logger.error("Error generating PDF receipt: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF receipt", e);
        }
    }

    /**
     * Add header section to PDF
     */
    private void addHeader(Document document, PdfFont boldFont, PdfFont normalFont, String receiptId) {
        // Platform name and title
        Paragraph platformNameParagraph = new Paragraph(platformName)
                .setFont(boldFont)
                .setFontSize(20)
                .setFontColor(ColorConstants.DARK_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
        document.add(platformNameParagraph);

        Paragraph title = new Paragraph("Subscription Payment Receipt")
                .setFont(boldFont)
                .setFontSize(16)
                .setFontColor(ColorConstants.BLUE)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15);
        document.add(title);

        // Receipt ID and Date
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        headerTable.addCell(new Cell().add(new Paragraph("Receipt ID:").setFont(boldFont)).setBorder(null));
        headerTable.addCell(new Cell().add(new Paragraph(receiptId).setFont(normalFont)).setBorder(null));

        headerTable.addCell(new Cell().add(new Paragraph("Transaction Date:").setFont(boldFont)).setBorder(null));
        headerTable.addCell(new Cell().add(new Paragraph(LocalDateTime.now().format(DATE_FORMATTER)).setFont(normalFont)).setBorder(null));

        document.add(headerTable);
    }

    /**
     * Add customer information section to PDF
     */
    private void addCustomerInfo(Document document, PdfFont boldFont, PdfFont normalFont, 
                                String userName, String userEmail, Long userId) {
        // Section title
        Paragraph sectionTitle = new Paragraph("Customer Information")
                .setFont(boldFont)
                .setFontSize(14)
                .setFontColor(ColorConstants.DARK_GRAY)
                .setMarginTop(20)
                .setMarginBottom(10);
        document.add(sectionTitle);

        // Customer details table
        Table customerTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        customerTable.addCell(new Cell().add(new Paragraph("Full Name:").setFont(boldFont)).setBorder(null));
        customerTable.addCell(new Cell().add(new Paragraph(userName).setFont(normalFont)).setBorder(null));

        customerTable.addCell(new Cell().add(new Paragraph("Email:").setFont(boldFont)).setBorder(null));
        customerTable.addCell(new Cell().add(new Paragraph(userEmail).setFont(normalFont)).setBorder(null));

        customerTable.addCell(new Cell().add(new Paragraph("User ID:").setFont(boldFont)).setBorder(null));
        customerTable.addCell(new Cell().add(new Paragraph(userId.toString()).setFont(normalFont)).setBorder(null));

        document.add(customerTable);
    }

    /**
     * Add subscription details section to PDF
     */
    private void addSubscriptionDetails(Document document, PdfFont boldFont, PdfFont normalFont, 
                                       UserSubscription subscription) {
        // Section title
        Paragraph sectionTitle = new Paragraph("Subscription Details")
                .setFont(boldFont)
                .setFontSize(14)
                .setFontColor(ColorConstants.DARK_GRAY)
                .setMarginTop(20)
                .setMarginBottom(10);
        document.add(sectionTitle);

        // Subscription details table
        Table subscriptionTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        SubscriptionPlan plan = subscription.getPlan();

        subscriptionTable.addCell(new Cell().add(new Paragraph("Plan Name:").setFont(boldFont)).setBorder(null));
        subscriptionTable.addCell(new Cell().add(new Paragraph(plan.getName().name()).setFont(normalFont)).setBorder(null));

        subscriptionTable.addCell(new Cell().add(new Paragraph("Duration:").setFont(boldFont)).setBorder(null));
        subscriptionTable.addCell(new Cell().add(new Paragraph(plan.getDurationDays() + " days").setFont(normalFont)).setBorder(null));

        subscriptionTable.addCell(new Cell().add(new Paragraph("Start Date:").setFont(boldFont)).setBorder(null));
        subscriptionTable.addCell(new Cell().add(new Paragraph(subscription.getSubscribedAt().format(SIMPLE_DATE_FORMATTER)).setFont(normalFont)).setBorder(null));

        subscriptionTable.addCell(new Cell().add(new Paragraph("Expiration Date:").setFont(boldFont)).setBorder(null));
        subscriptionTable.addCell(new Cell().add(new Paragraph(subscription.getExpiresAt().format(SIMPLE_DATE_FORMATTER)).setFont(normalFont)).setBorder(null));

        subscriptionTable.addCell(new Cell().add(new Paragraph("Status:").setFont(boldFont)).setBorder(null));
        subscriptionTable.addCell(new Cell().add(new Paragraph(subscription.getStatus().toString()).setFont(boldFont)).setFontColor(ColorConstants.GREEN).setBorder(null));

        document.add(subscriptionTable);
    }

    /**
     * Add payment details section to PDF
     */
    private void addPaymentDetails(Document document, PdfFont boldFont, PdfFont normalFont, 
                                   SubscriptionPlan plan, String transactionId) {
        // Section title
        Paragraph sectionTitle = new Paragraph("Payment Details")
                .setFont(boldFont)
                .setFontSize(14)
                .setFontColor(ColorConstants.DARK_GRAY)
                .setMarginTop(20)
                .setMarginBottom(10);
        document.add(sectionTitle);

        // Payment details table
        Table paymentTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        paymentTable.addCell(new Cell().add(new Paragraph("Payment Method:").setFont(boldFont)).setBorder(null));
        paymentTable.addCell(new Cell().add(new Paragraph("Simulated Card").setFont(normalFont)).setBorder(null));

        paymentTable.addCell(new Cell().add(new Paragraph("Card Number:").setFont(boldFont)).setBorder(null));
        paymentTable.addCell(new Cell().add(new Paragraph("****-****-****-" + generateRandomLast4()).setFont(normalFont)).setBorder(null));

        paymentTable.addCell(new Cell().add(new Paragraph("Amount Paid:").setFont(boldFont)).setBorder(null));
        paymentTable.addCell(new Cell().add(new Paragraph(plan.getPrice() + " USD").setFont(boldFont)).setFontColor(ColorConstants.DARK_GRAY).setBorder(null));

        paymentTable.addCell(new Cell().add(new Paragraph("Payment Status:").setFont(boldFont)).setBorder(null));
        paymentTable.addCell(new Cell().add(new Paragraph("SUCCESS").setFont(boldFont)).setFontColor(ColorConstants.GREEN).setBorder(null));

        paymentTable.addCell(new Cell().add(new Paragraph("Transaction ID:").setFont(boldFont)).setBorder(null));
        paymentTable.addCell(new Cell().add(new Paragraph(transactionId).setFont(normalFont)).setBorder(null));

        document.add(paymentTable);
    }

    /**
     * Add footer section to PDF
     */
    private void addFooter(Document document, PdfFont normalFont) {
        // Thank you message
        Paragraph thankYou = new Paragraph("Thank you for subscribing to " + platformName + "!")
                .setFont(normalFont)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(30)
                .setMarginBottom(10);
        document.add(thankYou);

        // Support email
        Paragraph support = new Paragraph("If you have any questions, contact us at: " + supportEmail)
                .setFont(normalFont)
                .setFontSize(10)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
        document.add(support);

        // Disclaimer
        Paragraph disclaimer = new Paragraph("This is a simulated payment receipt for demonstration purposes.")
                .setFont(normalFont)
                .setFontSize(9)
                .setFontColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
        document.add(disclaimer);
    }

    /**
     * Generate random last 4 digits for simulated card
     */
    private String generateRandomLast4() {
        int last4 = 1000 + (int)(Math.random() * 9000);
        return String.valueOf(last4);
    }

    /**
     * Generate a PDF invoice for an Invoice entity (Feature 3)
     */
    public byte[] generateInvoicePdf(tn.esprit.abonnement.entity.Invoice invoice) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            document.add(new Paragraph(platformName)
                    .setFont(boldFont).setFontSize(20)
                    .setFontColor(ColorConstants.DARK_GRAY)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(5));
            document.add(new Paragraph("INVOICE")
                    .setFont(boldFont).setFontSize(18)
                    .setFontColor(ColorConstants.BLUE)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(15));

            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(20);

            infoTable.addCell(new Cell().add(new Paragraph("Invoice Number:").setFont(boldFont)).setBorder(null));
            infoTable.addCell(new Cell().add(new Paragraph(invoice.getInvoiceNumber()).setFont(normalFont)).setBorder(null));

            infoTable.addCell(new Cell().add(new Paragraph("Issue Date:").setFont(boldFont)).setBorder(null));
            infoTable.addCell(new Cell().add(new Paragraph(invoice.getIssuedAt().format(DATE_FORMATTER)).setFont(normalFont)).setBorder(null));

            infoTable.addCell(new Cell().add(new Paragraph("User ID:").setFont(boldFont)).setBorder(null));
            infoTable.addCell(new Cell().add(new Paragraph(invoice.getUserId().toString()).setFont(normalFont)).setBorder(null));

            document.add(infoTable);

            document.add(new Paragraph("Subscription Details")
                    .setFont(boldFont).setFontSize(14)
                    .setFontColor(ColorConstants.DARK_GRAY).setMarginBottom(10));

            Table subTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(20);

            UserSubscription sub = invoice.getSubscription();
            String planName = sub != null && sub.getPlan() != null ? sub.getPlan().getName().name() : "N/A";
            String duration = sub != null && sub.getPlan() != null ? sub.getPlan().getDurationDays() + " days" : "N/A";
            String renewalDate = invoice.getRenewalDate() != null ? invoice.getRenewalDate().format(SIMPLE_DATE_FORMATTER) : "N/A";

            subTable.addCell(new Cell().add(new Paragraph("Plan Name:").setFont(boldFont)).setBorder(null));
            subTable.addCell(new Cell().add(new Paragraph(planName).setFont(normalFont)).setBorder(null));

            subTable.addCell(new Cell().add(new Paragraph("Duration:").setFont(boldFont)).setBorder(null));
            subTable.addCell(new Cell().add(new Paragraph(duration).setFont(normalFont)).setBorder(null));

            subTable.addCell(new Cell().add(new Paragraph("Renewal Date:").setFont(boldFont)).setBorder(null));
            subTable.addCell(new Cell().add(new Paragraph(renewalDate).setFont(normalFont)).setBorder(null));

            document.add(subTable);

            document.add(new Paragraph("Payment Details")
                    .setFont(boldFont).setFontSize(14)
                    .setFontColor(ColorConstants.DARK_GRAY).setMarginBottom(10));

            Table payTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(20);

            payTable.addCell(new Cell().add(new Paragraph("Amount Paid:").setFont(boldFont)).setBorder(null));
            payTable.addCell(new Cell().add(new Paragraph(invoice.getAmount() + " USD").setFont(boldFont))
                    .setFontColor(ColorConstants.DARK_GRAY).setBorder(null));

            payTable.addCell(new Cell().add(new Paragraph("Payment Status:").setFont(boldFont)).setBorder(null));
            payTable.addCell(new Cell().add(new Paragraph(invoice.isPaid() ? "PAID" : "UNPAID").setFont(boldFont))
                    .setFontColor(invoice.isPaid() ? ColorConstants.GREEN : ColorConstants.RED).setBorder(null));

            String sessionRef = invoice.getStripeSessionId() != null ? invoice.getStripeSessionId() : "N/A";
            payTable.addCell(new Cell().add(new Paragraph("Transaction Ref:").setFont(boldFont)).setBorder(null));
            payTable.addCell(new Cell().add(new Paragraph(sessionRef).setFont(normalFont)).setBorder(null));

            document.add(payTable);

            addFooter(document, normalFont);
            document.close();
            logger.info("Invoice PDF generated for invoice: {}", invoice.getInvoiceNumber());
            return baos.toByteArray();

        } catch (Exception e) {
            logger.error("Error generating invoice PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate invoice PDF", e);
        }
    }
}