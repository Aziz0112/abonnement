package tn.esprit.abonnement.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.abonnement.dto.InvoiceDTO;
import tn.esprit.abonnement.services.InvoiceService;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<InvoiceDTO>> getInvoicesForUser(@PathVariable Long userId) {
        return ResponseEntity.ok(invoiceService.getInvoicesForUser(userId));
    }

    @GetMapping("/{invoiceId}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long invoiceId) {
        byte[] pdfBytes = invoiceService.generateInvoicePdf(invoiceId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice-" + invoiceId + ".pdf");

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}
