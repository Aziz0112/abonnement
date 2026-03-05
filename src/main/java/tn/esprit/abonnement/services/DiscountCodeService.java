package tn.esprit.abonnement.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.abonnement.dto.DiscountValidationDTO;
import tn.esprit.abonnement.entity.DiscountCode;
import tn.esprit.abonnement.repository.DiscountCodeRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountCodeService {

    private final DiscountCodeRepository discountCodeRepository;

    /**
     * Admin: Create a new discount code
     */
    @Transactional
    public DiscountCode createDiscountCode(DiscountCode code) {
        log.info("Creating discount code: {}", code.getCode());
        return discountCodeRepository.save(code);
    }

    /**
     * Admin: List all active discount codes
     */
    public List<DiscountCode> getAllCodes() {
        return discountCodeRepository.findAllByIsActiveTrueOrderByCreatedAtDesc();
    }

    /**
     * Admin: Deactivate a discount code
     */
    @Transactional
    public void deactivateCode(Long id) {
        DiscountCode code = discountCodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Discount code not found with id: " + id));
        
        log.info("Deactivating discount code: {}", code.getCode());
        code.setActive(false);
        discountCodeRepository.save(code);
    }

    /**
     * Public: Validate a discount code and return discount details
     */
    public DiscountValidationDTO validateCode(String code) {
        DiscountCode discount = discountCodeRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Invalid discount code"));

        // Check if code is active
        if (!discount.isActive()) {
            throw new RuntimeException("Discount code is no longer active");
        }

        // Check if code has expired
        if (discount.getExpiresAt() != null && 
            discount.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Discount code has expired");
        }

        // Check if code has reached maximum uses
        if (discount.getMaxUses() != null && 
            discount.getUsesCount() >= discount.getMaxUses()) {
            throw new RuntimeException("Discount code has reached maximum uses");
        }

        log.info("Discount code validated successfully: {} ({}% off)", 
                 code, discount.getDiscountPercentage());

        return DiscountValidationDTO.builder()
                .code(discount.getCode())
                .discountPercentage(discount.getDiscountPercentage())
                .build();
    }

    /**
     * Mark a discount code as used
     */
    @Transactional
    public void useCode(String code) {
        DiscountCode discount = discountCodeRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Discount code not found"));

        log.info("Marking discount code as used: {}", code);
        discount.setUsesCount(discount.getUsesCount() + 1);
        discountCodeRepository.save(discount);
    }
}