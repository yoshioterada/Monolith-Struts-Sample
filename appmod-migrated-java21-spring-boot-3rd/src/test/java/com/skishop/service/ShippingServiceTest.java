package com.skishop.service;

import com.skishop.model.OrderShipping;
import com.skishop.repository.OrderShippingRepository;
import com.skishop.repository.ShippingMethodRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ShippingServiceTest {

    @Mock
    private ShippingMethodRepository shippingMethodRepository;

    @Mock
    private OrderShippingRepository orderShippingRepository;

    @InjectMocks
    private ShippingService shippingService;

    @Test
    @DisplayName("1万円未満の場合、送料800円を返す")
    void should_returnDefaultFee_when_amountBelowThreshold() {
        // Arrange
        var amount = new BigDecimal("9999");

        // Act
        var result = shippingService.calculateShippingFee(amount);

        // Assert
        assertThat(result).isEqualByComparingTo(new BigDecimal("800"));
    }

    @Test
    @DisplayName("1万円以上の場合、送料無料（0円）を返す")
    void should_returnZero_when_amountAtOrAboveThreshold() {
        // Arrange
        var amount = new BigDecimal("10000");

        // Act
        var result = shippingService.calculateShippingFee(amount);

        // Assert
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("金額がnullの場合、デフォルト送料を返す")
    void should_returnDefaultFee_when_amountIsNull() {
        // Act
        var result = shippingService.calculateShippingFee(null);

        // Assert
        assertThat(result).isEqualByComparingTo(new BigDecimal("800"));
    }

    @Test
    @DisplayName("配送情報が存在する場合、DBに保存する")
    void should_save_when_shippingIsNotNull() {
        // Arrange
        var shipping = new OrderShipping();
        shipping.setId("ship-1");

        // Act
        shippingService.saveOrderShipping(shipping);

        // Assert
        verify(orderShippingRepository).save(shipping);
    }

    @Test
    @DisplayName("配送情報がnullの場合、DBに保存しない")
    void should_notSave_when_shippingIsNull() {
        // Act
        shippingService.saveOrderShipping(null);

        // Assert
        verify(orderShippingRepository, never()).save(any());
    }
}
