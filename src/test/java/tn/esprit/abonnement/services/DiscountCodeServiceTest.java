package tn.esprit.abonnement.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.abonnement.dto.DiscountValidationDTO;
import tn.esprit.abonnement.entity.DiscountCode;
import tn.esprit.abonnement.exception.BadRequestException;
import tn.esprit.abonnement.exception.ResourceNotFoundException;
import tn.esprit.abonnement.repository.DiscountCodeRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscountCodeServiceTest {

    @Mock
    private DiscountCodeRepository discountCodeRepository;

    @InjectMocks
    private DiscountCodeService discountCodeService;

    private DiscountCode buildActiveCode(String code, int percentage) {
        DiscountCode dc = new DiscountCode();
        dc.setCode(code);
        dc.setDiscountPercentage(percentage);
        dc.setMaxUses(10);
        dc.setUsesCount(0);
        dc.setIsActive(true);
        return dc;
    }

    // ── createDiscountCode() ──────────────────────────────────────────────────

    @Test
    void createDiscountCode_withValidCode_returnsSavedCode() {
        DiscountCode code = buildActiveCode("SAVE20", 20);
        when(discountCodeRepository.findByCode("SAVE20")).thenReturn(Optional.empty());
        when(discountCodeRepository.save(code)).thenReturn(code);

        DiscountCode result = discountCodeService.createDiscountCode(code);

        assertThat(result.getCode()).isEqualTo("SAVE20");
        assertThat(result.getDiscountPercentage()).isEqualTo(20);
        verify(discountCodeRepository).save(code);
    }

    @Test
    void createDiscountCode_withZeroPercentage_throwsIllegalArgumentException() {
        DiscountCode code = buildActiveCode("ZERO", 0);

        assertThatThrownBy(() -> discountCodeService.createDiscountCode(code))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Discount percentage must be between 1 and 100");
    }

    @Test
    void createDiscountCode_withOver100Percentage_throwsIllegalArgumentException() {
        DiscountCode code = buildActiveCode("OVER", 101);

        assertThatThrownBy(() -> discountCodeService.createDiscountCode(code))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Discount percentage must be between 1 and 100");
    }

    @Test
    void createDiscountCode_withNullPercentage_throwsIllegalArgumentException() {
        DiscountCode code = buildActiveCode("NULL", 50);
        code.setDiscountPercentage(null);

        assertThatThrownBy(() -> discountCodeService.createDiscountCode(code))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Discount percentage must be between 1 and 100");
    }

    @Test
    void createDiscountCode_withDuplicateCode_throwsIllegalArgumentException() {
        DiscountCode code = buildActiveCode("SAVE20", 20);
        when(discountCodeRepository.findByCode("SAVE20")).thenReturn(Optional.of(code));

        assertThatThrownBy(() -> discountCodeService.createDiscountCode(code))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createDiscountCode_withNullUsesCount_initializesItToZero() {
        DiscountCode code = buildActiveCode("NEW10", 10);
        code.setUsesCount(null);
        when(discountCodeRepository.findByCode("NEW10")).thenReturn(Optional.empty());
        when(discountCodeRepository.save(code)).thenReturn(code);

        discountCodeService.createDiscountCode(code);

        assertThat(code.getUsesCount()).isEqualTo(0);
    }

    @Test
    void createDiscountCode_withIsActiveNull_setsItToTrue() {
        DiscountCode code = buildActiveCode("ACT", 15);
        code.setIsActive(null);
        when(discountCodeRepository.findByCode("ACT")).thenReturn(Optional.empty());
        when(discountCodeRepository.save(code)).thenReturn(code);

        discountCodeService.createDiscountCode(code);

        assertThat(code.getIsActive()).isTrue();
    }

    // ── validateCode() ────────────────────────────────────────────────────────

    @Test
    void validateCode_withValidCode_returnsDiscountDTO() {
        DiscountCode code = buildActiveCode("PROMO10", 10);
        when(discountCodeRepository.findByCode("PROMO10")).thenReturn(Optional.of(code));

        DiscountValidationDTO result = discountCodeService.validateCode("PROMO10");

        assertThat(result.getCode()).isEqualTo("PROMO10");
        assertThat(result.getDiscountPercentage()).isEqualTo(10);
    }

    @Test
    void validateCode_withNonExistentCode_throwsResourceNotFoundException() {
        when(discountCodeRepository.findByCode("FAKE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> discountCodeService.validateCode("FAKE"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void validateCode_withInactiveCode_throwsBadRequestException() {
        DiscountCode code = buildActiveCode("INACTIVE", 15);
        code.setIsActive(false);
        when(discountCodeRepository.findByCode("INACTIVE")).thenReturn(Optional.of(code));

        assertThatThrownBy(() -> discountCodeService.validateCode("INACTIVE"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no longer active");
    }

    @Test
    void validateCode_withMaxUsesReached_throwsBadRequestException() {
        DiscountCode code = buildActiveCode("MAXED", 25);
        code.setMaxUses(5);
        code.setUsesCount(5);
        when(discountCodeRepository.findByCode("MAXED")).thenReturn(Optional.of(code));

        assertThatThrownBy(() -> discountCodeService.validateCode("MAXED"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("maximum uses");
    }

    @Test
    void validateCode_withUsesCountBelowMax_doesNotThrow() {
        DiscountCode code = buildActiveCode("OK", 30);
        code.setMaxUses(10);
        code.setUsesCount(9);
        when(discountCodeRepository.findByCode("OK")).thenReturn(Optional.of(code));

        DiscountValidationDTO result = discountCodeService.validateCode("OK");

        assertThat(result).isNotNull();
        assertThat(result.getDiscountPercentage()).isEqualTo(30);
    }

    @Test
    void validateCode_withNullMaxUses_doesNotCheckLimit() {
        DiscountCode code = buildActiveCode("UNLIMITED", 50);
        code.setMaxUses(null);
        when(discountCodeRepository.findByCode("UNLIMITED")).thenReturn(Optional.of(code));

        DiscountValidationDTO result = discountCodeService.validateCode("UNLIMITED");

        assertThat(result.getCode()).isEqualTo("UNLIMITED");
    }

    // ── deactivateCode() ──────────────────────────────────────────────────────

    @Test
    void deactivateCode_withExistingId_setsIsActiveFalse() {
        DiscountCode code = buildActiveCode("ACTIVE", 20);
        code.setIsActive(true);
        when(discountCodeRepository.findById(1L)).thenReturn(Optional.of(code));

        discountCodeService.deactivateCode(1L);

        assertThat(code.getIsActive()).isFalse();
        verify(discountCodeRepository).save(code);
    }

    @Test
    void deactivateCode_withNonExistentId_throwsRuntimeException() {
        when(discountCodeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> discountCodeService.deactivateCode(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    // ── getAllCodes() ──────────────────────────────────────────────────────────

    @Test
    void getAllCodes_returnsOnlyActiveCodes() {
        List<DiscountCode> codes = List.of(
                buildActiveCode("CODE_A", 10),
                buildActiveCode("CODE_B", 20)
        );
        when(discountCodeRepository.findAllByIsActiveTrueOrderByCreatedAtDesc()).thenReturn(codes);

        List<DiscountCode> result = discountCodeService.getAllCodes();

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(c -> c.getIsActive());
    }

    @Test
    void getAllCodes_withNoCodes_returnsEmptyList() {
        when(discountCodeRepository.findAllByIsActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of());

        List<DiscountCode> result = discountCodeService.getAllCodes();

        assertThat(result).isEmpty();
    }

    // ── useCode() ─────────────────────────────────────────────────────────────

    @Test
    void useCode_incrementsUsesCountByOne() {
        DiscountCode code = buildActiveCode("USE", 15);
        code.setUsesCount(2);
        when(discountCodeRepository.findByCode("USE")).thenReturn(Optional.of(code));

        discountCodeService.useCode("USE");

        assertThat(code.getUsesCount()).isEqualTo(3);
        verify(discountCodeRepository).save(code);
    }

    @Test
    void useCode_withNonExistentCode_throwsRuntimeException() {
        when(discountCodeRepository.findByCode("GHOST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> discountCodeService.useCode("GHOST"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }
}
