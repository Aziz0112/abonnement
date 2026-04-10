package tn.esprit.abonnement.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.abonnement.dto.DiscountValidationDTO;
import tn.esprit.abonnement.entity.DiscountCode;
import tn.esprit.abonnement.services.DiscountCodeService;

import java.util.List;

@RestController
@RequestMapping("/api/abonnements/discounts")
@RequiredArgsConstructor
@Slf4j
public class DiscountCodeController {

    private final DiscountCodeService discountCodeService;

    /**
     * Admin: Create a new discount code
     */
    @PostMapping
    public ResponseEntity<DiscountCode> createDiscountCode(@RequestBody DiscountCode code) {
        log.info("Creating discount code request: code={}, discountPercentage={}, maxUses={}", 
                 code.getCode(), code.getDiscountPercentage(), code.getMaxUses());
        return ResponseEntity.ok(discountCodeService.createDiscountCode(code));
    }

    /**
     * Admin: Get all active discount codes
     */
    @GetMapping
    public ResponseEntity<List<DiscountCode>> getAllCodes() {
        return ResponseEntity.ok(discountCodeService.getAllCodes());
    }

    /**
     * Admin: Deactivate a discount code
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateCode(@PathVariable Long id) {
        discountCodeService.deactivateCode(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Public: Validate a discount code (used by checkout)
     */
    @GetMapping("/validate/{code}")
    public ResponseEntity<DiscountValidationDTO> validateCode(@PathVariable String code) {
        return ResponseEntity.ok(discountCodeService.validateCode(code));
    }
}