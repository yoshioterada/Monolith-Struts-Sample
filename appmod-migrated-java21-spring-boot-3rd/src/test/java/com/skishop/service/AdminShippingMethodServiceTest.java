package com.skishop.service;

import com.skishop.dto.request.admin.AdminShippingMethodRequest;
import com.skishop.exception.ResourceNotFoundException;
import com.skishop.model.ShippingMethod;
import com.skishop.repository.ShippingMethodRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminShippingMethodServiceTest {

    @Mock
    private ShippingMethodRepository shippingMethodRepository;

    @InjectMocks
    private AdminShippingMethodService adminShippingMethodService;

    private AdminShippingMethodRequest createRequest() {
        return new AdminShippingMethodRequest(null, "STANDARD", "標準配送", new BigDecimal("800"), true, 1);
    }

    @Test
    @DisplayName("全配送方法を取得できる")
    void should_returnAll_when_listAll() {
        // Arrange
        var method = new ShippingMethod();
        method.setId("sm-1");
        when(shippingMethodRepository.findAll()).thenReturn(List.of(method));

        // Act
        var result = adminShippingMethodService.listAll();

        // Assert
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("有効なIDで配送方法を取得できる")
    void should_returnMethod_when_idExists() {
        // Arrange
        var method = new ShippingMethod();
        method.setId("sm-2");
        when(shippingMethodRepository.findById("sm-2")).thenReturn(Optional.of(method));

        // Act
        var result = adminShippingMethodService.findById("sm-2");

        // Assert
        assertThat(result.getId()).isEqualTo("sm-2");
    }

    @Test
    @DisplayName("存在しないIDの場合、ResourceNotFoundExceptionをスローする")
    void should_throwException_when_idNotFound() {
        // Arrange
        when(shippingMethodRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> adminShippingMethodService.findById("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("配送方法を作成できる")
    void should_createMethod_when_requestProvided() {
        // Arrange
        var request = createRequest();
        when(shippingMethodRepository.save(any(ShippingMethod.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        var result = adminShippingMethodService.create(request);

        // Assert
        assertThat(result.getId()).isNotNull();
        assertThat(result.getCode()).isEqualTo("STANDARD");
        assertThat(result.getName()).isEqualTo("標準配送");
        assertThat(result.getFee()).isEqualByComparingTo(new BigDecimal("800"));
        assertThat(result.isActive()).isTrue();
    }

    @Test
    @DisplayName("配送方法を更新できる")
    void should_updateMethod_when_idAndRequestProvided() {
        // Arrange
        var existing = new ShippingMethod();
        existing.setId("sm-3");
        existing.setCode("OLD_CODE");
        when(shippingMethodRepository.findById("sm-3")).thenReturn(Optional.of(existing));
        when(shippingMethodRepository.save(any(ShippingMethod.class))).thenAnswer(i -> i.getArgument(0));
        var request = new AdminShippingMethodRequest(null, "EXPRESS", "速達配送", new BigDecimal("1200"), true, 2);

        // Act
        var result = adminShippingMethodService.update("sm-3", request);

        // Assert
        assertThat(result.getCode()).isEqualTo("EXPRESS");
        assertThat(result.getName()).isEqualTo("速達配送");
    }

    @Test
    @DisplayName("存在しないIDを更新しようとした場合、ResourceNotFoundExceptionをスローする")
    void should_throwException_when_updateNonExistent() {
        // Arrange
        when(shippingMethodRepository.findById("none")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> adminShippingMethodService.update("none", createRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("配送方法を削除できる")
    void should_delete_when_idExists() {
        // Arrange
        var method = new ShippingMethod();
        method.setId("sm-4");
        when(shippingMethodRepository.findById("sm-4")).thenReturn(Optional.of(method));

        // Act
        adminShippingMethodService.delete("sm-4");

        // Assert
        verify(shippingMethodRepository).delete(method);
    }

    @Test
    @DisplayName("存在しないIDを削除しようとした場合、ResourceNotFoundExceptionをスローする")
    void should_throwException_when_deleteNonExistent() {
        // Arrange
        when(shippingMethodRepository.findById("none")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> adminShippingMethodService.delete("none"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
