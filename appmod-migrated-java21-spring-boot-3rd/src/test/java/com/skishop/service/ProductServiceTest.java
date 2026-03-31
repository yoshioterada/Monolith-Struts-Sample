package com.skishop.service;

import com.skishop.exception.ResourceNotFoundException;
import com.skishop.model.Category;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

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

    @Test
    @DisplayName("有効な商品IDで商品を取得した場合、商品を返す")
    void should_returnProduct_when_productIdExists() {
        // Arrange
        var product = new Product();
        product.setId("P001");
        product.setName("Alpine Ski");
        when(productRepository.findById("P001")).thenReturn(Optional.of(product));

        // Act
        var result = productService.findById("P001");

        // Assert
        assertThat(result.getId()).isEqualTo("P001");
        assertThat(result.getName()).isEqualTo("Alpine Ski");
    }

    @Test
    @DisplayName("存在しない商品IDの場合、ResourceNotFoundExceptionをスローする")
    void should_throwException_when_productIdNotFound() {
        // Arrange
        when(productRepository.findById(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.findById("NONEXISTENT"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("キーワードで商品を検索した場合、条件に合う商品リストを返す")
    void should_returnProducts_when_keywordSearched() {
        // Arrange
        var pageable = PageRequest.of(0, 10);
        var products = List.of(new Product(), new Product());
        when(productRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(products, pageable, 2));

        // Act
        var result = productService.search("ski", null, pageable);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("キーワードがnullの場合、全商品を対象に検索する")
    void should_searchAll_when_keywordIsNull() {
        // Arrange
        var pageable = PageRequest.of(0, 10);
        when(productRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // Act
        var result = productService.search(null, null, pageable);

        // Assert
        assertThat(result).isNotNull();
        verify(productRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    @DisplayName("カテゴリIDで商品を検索した場合、一致する商品リストを返す")
    void should_returnProducts_when_categoryIdSearched() {
        // Arrange
        var products = List.of(new Product(), new Product());
        when(productRepository.findByCategoryId("cat-1")).thenReturn(products);

        // Act
        var result = productService.findByCategoryId("cat-1");

        // Assert
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("ステータスで商品を検索した場合、一致する商品リストを返す")
    void should_returnProducts_when_statusSearched() {
        // Arrange
        var products = List.of(new Product());
        when(productRepository.findByStatus("ACTIVE")).thenReturn(products);

        // Act
        var result = productService.findByStatus("ACTIVE");

        // Assert
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("商品を無効化した場合、ステータスがINACTIVEになる")
    void should_setInactive_when_deactivateProduct() {
        // Arrange
        var product = new Product();
        product.setId("P002");
        product.setStatus("ACTIVE");
        when(productRepository.findById("P002")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        when(inventoryRepository.findByProductId("P002")).thenReturn(Optional.empty());

        // Act
        productService.deactivateProduct("P002");

        // Assert
        var captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    @DisplayName("商品を無効化した際に在庫があれば在庫もゼロにする")
    void should_zeroInventory_when_deactivateProductWithInventory() {
        // Arrange
        var product = new Product();
        product.setId("P003");
        product.setStatus("ACTIVE");
        var inventory = new Inventory();
        inventory.setQuantity(10);
        when(productRepository.findById("P003")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        when(inventoryRepository.findByProductId("P003")).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        productService.deactivateProduct("P003");

        // Assert
        var captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantity()).isEqualTo(0);
        assertThat(captor.getValue().getStatus()).isEqualTo("OUT_OF_STOCK");
    }

    @Test
    @DisplayName("存在しない商品を無効化した場合、ResourceNotFoundExceptionをスローする")
    void should_throwException_when_deactivateNonExistentProduct() {
        // Arrange
        when(productRepository.findById(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.deactivateProduct("NONEXISTENT"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
