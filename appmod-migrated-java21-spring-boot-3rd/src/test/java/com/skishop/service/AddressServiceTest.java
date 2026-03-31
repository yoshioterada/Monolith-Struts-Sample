package com.skishop.service;

import com.skishop.exception.ResourceNotFoundException;
import com.skishop.model.Address;
import com.skishop.repository.AddressRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private AddressService addressService;

    @Test
    @DisplayName("ユーザーIDで住所を検索した場合、住所リストを返す")
    void should_returnAddresses_when_userIdExists() {
        // Arrange
        var addr1 = new Address();
        addr1.setId("a-1");
        var addr2 = new Address();
        addr2.setId("a-2");
        when(addressRepository.findByUserId("u-1")).thenReturn(List.of(addr1, addr2));

        // Act
        var result = addressService.findByUserId("u-1");

        // Assert
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("有効なIDで住所を取得した場合、住所を返す")
    void should_returnAddress_when_idExists() {
        // Arrange
        var addr = new Address();
        addr.setId("a-3");
        when(addressRepository.findById("a-3")).thenReturn(Optional.of(addr));

        // Act
        var result = addressService.findById("a-3");

        // Assert
        assertThat(result.getId()).isEqualTo("a-3");
    }

    @Test
    @DisplayName("存在しないIDで住所を取得した場合、ResourceNotFoundExceptionをスローする")
    void should_throwException_when_addressIdNotFound() {
        // Arrange
        when(addressRepository.findById(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> addressService.findById("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("IDなしで住所を保存した場合、UUIDが自動設定される")
    void should_generateId_when_saveWithoutId() {
        // Arrange
        var addr = new Address();
        when(addressRepository.save(any(Address.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        var result = addressService.save(addr);

        // Assert
        assertThat(result.getId()).isNotNull();
    }

    @Test
    @DisplayName("IDありで住所を保存した場合、そのIDで保存される")
    void should_keepExistingId_when_saveWithId() {
        // Arrange
        var addr = new Address();
        addr.setId("a-fixed");
        when(addressRepository.save(any(Address.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        var result = addressService.save(addr);

        // Assert
        assertThat(result.getId()).isEqualTo("a-fixed");
    }

    @Test
    @DisplayName("住所を削除した場合、リポジトリのdeleteByIdが呼ばれる")
    void should_callDeleteById_when_deleteAddress() {
        // Act
        addressService.delete("a-5");

        // Assert
        verify(addressRepository).deleteById("a-5");
    }
}
