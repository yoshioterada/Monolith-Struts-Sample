package com.skishop.service;

import com.skishop.exception.BusinessException;
import com.skishop.model.Coupon;
import com.skishop.model.CouponUsage;
import com.skishop.repository.CouponRepository;
import com.skishop.repository.CouponUsageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponUsageRepository couponUsageRepository;

    @InjectMocks
    private CouponService couponService;

    @Test
    @DisplayName("有効なクーポンコードでクーポンが返される")
    void should_returnCoupon_when_validCode() {
        // Arrange
        var coupon = new Coupon();
        coupon.setId("coupon-1");
        coupon.setCode("SAVE10");
        coupon.setActive(true);
        coupon.setUsageLimit(100);
        coupon.setUsedCount(5);
        coupon.setExpiresAt(LocalDateTime.now().plusDays(30));
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));

        // Act
        var result = couponService.validateCoupon("SAVE10", new BigDecimal("10000"));

        // Assert
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getCode()).isEqualTo("SAVE10");
    }

    @Test
    @DisplayName("空のクーポンコードの場合、空の Optional を返す")
    void should_returnNull_when_codeIsBlank() {
        // Act
        var result = couponService.validateCoupon("", new BigDecimal("10000"));

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("nullのクーポンコードの場合、空の Optional を返す")
    void should_returnNull_when_codeIsNull() {
        // Act
        var result = couponService.validateCoupon(null, new BigDecimal("10000"));

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("存在しないクーポンコードの場合、例外をスローする")
    void should_throwException_when_couponNotFound() {
        // Arrange
        when(couponRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> couponService.validateCoupon("INVALID", new BigDecimal("10000")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("無効化されたクーポンの場合、例外をスローする")
    void should_throwException_when_couponInactive() {
        // Arrange
        var coupon = new Coupon();
        coupon.setActive(false);
        when(couponRepository.findByCode("EXPIRED")).thenReturn(Optional.of(coupon));

        // Act & Assert
        assertThatThrownBy(() -> couponService.validateCoupon("EXPIRED", new BigDecimal("10000")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("使用上限に達したクーポンの場合、例外をスローする")
    void should_throwException_when_couponUsageLimitReached() {
        // Arrange
        var coupon = new Coupon();
        coupon.setActive(true);
        coupon.setUsageLimit(10);
        coupon.setUsedCount(10);
        when(couponRepository.findByCode("MAXED")).thenReturn(Optional.of(coupon));

        // Act & Assert
        assertThatThrownBy(() -> couponService.validateCoupon("MAXED", new BigDecimal("10000")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("パーセント割引が正しく計算される")
    void should_calculatePercentDiscount_when_percentType() {
        // Arrange
        var coupon = new Coupon();
        coupon.setCouponType("PERCENT");
        coupon.setDiscountValue(new BigDecimal("10"));

        // Act
        var result = couponService.calculateDiscount(coupon, new BigDecimal("10000"));

        // Assert
        assertThat(result).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("固定額割引が正しく計算される")
    void should_calculateFixedDiscount_when_fixedType() {
        // Arrange
        var coupon = new Coupon();
        coupon.setCouponType("FIXED");
        coupon.setDiscountValue(new BigDecimal("500"));

        // Act
        var result = couponService.calculateDiscount(coupon, new BigDecimal("10000"));

        // Assert
        assertThat(result).isEqualByComparingTo(new BigDecimal("500"));
    }

    @Test
    @DisplayName("割引が最大割引額を超えない")
    void should_capDiscount_when_exceedsMaximum() {
        // Arrange
        var coupon = new Coupon();
        coupon.setCouponType("PERCENT");
        coupon.setDiscountValue(new BigDecimal("50"));
        coupon.setMaximumDiscount(new BigDecimal("2000"));

        // Act
        var result = couponService.calculateDiscount(coupon, new BigDecimal("10000"));

        // Assert
        assertThat(result).isEqualByComparingTo(new BigDecimal("2000"));
    }

    @Test
    @DisplayName("nullクーポンの場合、割引が0になる")
    void should_returnZero_when_couponIsNull() {
        // Act
        var result = couponService.calculateDiscount(null, new BigDecimal("10000"));

        // Assert
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("クーポン使用記録が作成される")
    void should_createUsageRecord_when_markUsed() {
        // Arrange
        var coupon = new Coupon();
        coupon.setId("coupon-1");
        coupon.setUsedCount(5);
        when(couponRepository.save(any(Coupon.class))).thenAnswer(i -> i.getArgument(0));
        when(couponUsageRepository.save(any(CouponUsage.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        couponService.markUsed(coupon, "user-1", "order-1", new BigDecimal("1000"));

        // Assert
        assertThat(coupon.getUsedCount()).isEqualTo(6);
        verify(couponUsageRepository).save(any(CouponUsage.class));
    }
}
