package tn.esprit.abonnement.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.abonnement.dto.InvoiceDTO;
import tn.esprit.abonnement.entity.Invoice;
import tn.esprit.abonnement.entity.UserSubscription;
import tn.esprit.abonnement.repository.InvoiceRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PdfService pdfService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Transactional
    public Invoice createInvoiceForSubscription(UserSubscription sub, String stripeSessionId) {
        String invoiceNumber = "INV-" + LocalDateTime.now().format(DATE_FMT) + "-" +
                String.format("%04d", sub.getId());

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .subscription(sub)
                .userId(sub.getUserId())
                .amount(sub.getPlan().getPrice())
                .issuedAt(LocalDateTime.now())
                .renewalDate(sub.getExpiresAt())
                .stripeSessionId(stripeSessionId)
                .paid(true)
                .build();

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice created: {} for subscription {}", invoiceNumber, sub.getId());
        return saved;
    }

    public List<InvoiceDTO> getInvoicesForUser(Long userId) {
        return invoiceRepository.findByUserIdOrderByIssuedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public byte[] generateInvoicePdf(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + invoiceId));
        return pdfService.generateInvoicePdf(invoice);
    }

    private InvoiceDTO toDTO(Invoice invoice) {
        String planName = "";
        String subscriptionStatus = "";
        if (invoice.getSubscription() != null) {
            if (invoice.getSubscription().getPlan() != null) {
                planName = invoice.getSubscription().getPlan().getName().name();
            }
            if (invoice.getSubscription().getStatus() != null) {
                subscriptionStatus = invoice.getSubscription().getStatus().name();
            }
        }

        return InvoiceDTO.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .planName(planName)
                .amount(invoice.getAmount())
                .issuedAt(invoice.getIssuedAt() != null ? invoice.getIssuedAt().format(DISPLAY_FMT) : "")
                .renewalDate(invoice.getRenewalDate() != null ? invoice.getRenewalDate().format(DISPLAY_FMT) : "")
                .subscriptionStatus(subscriptionStatus)
                .paid(invoice.isPaid())
                .stripeSessionId(invoice.getStripeSessionId())
                .build();
    }
}
