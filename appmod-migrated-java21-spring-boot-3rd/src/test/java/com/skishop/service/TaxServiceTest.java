package com.skishop.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TaxServiceTest {

    private final TaxService taxService = new TaxService("0.10");

    @Test
    @DisplayName("金額に消費税率10%を掛けた税額を返す")
    void should_returnTax_when_amountProvided() {
        // Arrange
        var amount = new BigDecimal("10000");

        // Act
        var result = taxService.calculateTax(amount);

        // Assert
        assertThat(result).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("nullが渡された場合、ZEROを返す")
    void should_returnZero_when_amountIsNull() {
        // Act
        var result = taxService.calculateTax(null);

        // Assert
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("ゼロ金額の場合、ゼロを返す")
    void should_returnZero_when_amountIsZero() {
        // Act
        var result = taxService.calculateTax(BigDecimal.ZERO);

        // Assert
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("小数点のある金額の場合、HALFUPで端数処理されたtaxを返す")
    void should_roundHalfUp_when_amountHasFraction() {
        // Arrange
        var amount = new BigDecimal("333");

        // Act
        var result = taxService.calculateTax(amount);

        // Assert
        assertThat(result).isEqualByComparingTo(new BigDecimal("33.30"));
    }
}
