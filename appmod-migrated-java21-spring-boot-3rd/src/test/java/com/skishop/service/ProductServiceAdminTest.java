package com.skishop.service;

import com.skishop.dto.request.admin.AdminProductRequest;
import com.skishop.exception.ResourceNotFoundException;
import com.skishop.model.Inventory;
import com.skishop.model.Price;
import com.skishop.model.Product;
import com.skishop.repository.CategoryRepository;
import com.skishop.repository.InventoryRepository;
import com.skishop.repository.PriceRepository;
import com.skishop.repository.ProductRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Additional ProductService tests covering createProduct and updateProduct.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceAdminTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private PriceRepository priceRepository;

    @InjectMocks
    private ProductService productService;

    private AdminProductRequest createRequest(int qty) {
        return new AdminProductRequest(null, "Ski Boots", "Brand", "Description",
                "cat-1", new BigDecimal("25000"), new BigDecimal("20000"), "ACTIVE", qty);
    }

    @Test
    @DisplayName("商品を作成した場合、商品・価格・在庫レコードが保存される")
    void should_createProductWithPriceAndInventory_when_createCalled() {
        // Arrange
        var request = createRequest(10);
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        when(priceRepository.save(any(Price.class))).thenAnswer(i -> i.getArgument(0));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        var result = productService.createProduct(request);

        // Assert
        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("Ski Boots");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        verify(priceRepository).save(any(Price.class));
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    @DisplayName("在庫0で商品を作成した場合、在庫ステータスがOUT_OF_STOCKになる")
    void should_setOutOfStock_when_createWithZeroInventory() {
        // Arrange
        var request = createRequest(0);
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        when(priceRepository.save(any(Price.class))).thenAnswer(i -> i.getArgument(0));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        productService.createProduct(request);

        // Assert
        var captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("OUT_OF_STOCK");
    }

    @Test
    @DisplayName("商品を更新した場合、商品・価格・在庫が更新される")
    void should_updateProductWithPriceAndInventory_when_updateCalled() {
        // Arrange
        var product = new Product();
        product.setId("P100");
        var price = new Price();
        price.setProductId("P100");
        var inventory = new Inventory();
        inventory.setProductId("P100");
        inventory.setQuantity(5);

        when(productRepository.findById("P100")).thenReturn(Optional.of(product));
        when(priceRepository.findByProductId("P100")).thenReturn(List.of(price));
        when(priceRepository.save(any(Price.class))).thenAnswer(i -> i.getArgument(0));
        when(inventoryRepository.findByProductId("P100")).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        var request = new AdminProductRequest("P100", "Updated Name", "NewBrand",
                "New desc", "cat-2", new BigDecimal("30000"), new BigDecimal("25000"), "ACTIVE", 20);

        // Act
        var result = productService.updateProduct("P100", request);

        // Assert
        assertThat(result.getName()).isEqualTo("Updated Name");
        verify(priceRepository).save(any(Price.class));
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    @DisplayName("存在しない商品IDを更新した場合、ResourceNotFoundExceptionをスローする")
    void should_throwException_when_updateNonExistentProduct() {
        // Arrange
        when(productRepository.findById(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.updateProduct("NONE", createRequest(5)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
