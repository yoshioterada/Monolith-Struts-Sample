package com.skishop.repository;

import com.skishop.model.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    private Product createProduct(String id, String name, String categoryId, String status) {
        var product = new Product();
        product.setId(id);
        product.setName(name);
        product.setBrand("TestBrand");
        product.setSku("SKU-" + id);
        product.setCategoryId(categoryId);
        product.setStatus(status);
        return product;
    }

    @Test
    @DisplayName("カテゴリIDで商品を検索した場合、一致する商品リストを返す")
    void should_returnProducts_when_categoryIdMatches() {
        // Arrange
        var p1 = createProduct("P001", "Alpine Ski", "cat-1", "ACTIVE");
        var p2 = createProduct("P002", "Nordic Ski", "cat-1", "ACTIVE");
        var p3 = createProduct("P003", "Snowboard", "cat-2", "ACTIVE");
        entityManager.persistAndFlush(p1);
        entityManager.persistAndFlush(p2);
        entityManager.persistAndFlush(p3);

        // Act
        List<Product> cat1Products = productRepository.findByCategoryId("cat-1");

        // Assert
        assertThat(cat1Products).hasSize(2);
        assertThat(cat1Products).extracting(Product::getName)
                .containsExactlyInAnyOrder("Alpine Ski", "Nordic Ski");
    }

    @Test
    @DisplayName("該当カテゴリに商品が存在しない場合、空のリストを返す")
    void should_returnEmptyList_when_categoryHasNoProducts() {
        // Arrange
        var product = createProduct("P004", "Test Product", "cat-1", "ACTIVE");
        entityManager.persistAndFlush(product);

        // Act
        List<Product> result = productRepository.findByCategoryId("cat-999");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("ステータスで商品を検索した場合、一致する商品リストを返す")
    void should_returnProducts_when_statusMatches() {
        // Arrange
        var active = createProduct("P005", "Active Product", "cat-1", "ACTIVE");
        var inactive = createProduct("P006", "Inactive Product", "cat-1", "INACTIVE");
        entityManager.persistAndFlush(active);
        entityManager.persistAndFlush(inactive);

        // Act
        List<Product> activeProducts = productRepository.findByStatus("ACTIVE");
        List<Product> inactiveProducts = productRepository.findByStatus("INACTIVE");

        // Assert
        assertThat(activeProducts).extracting(Product::getId).contains("P005");
        assertThat(inactiveProducts).extracting(Product::getId).contains("P006");
    }

    @Test
    @DisplayName("商品を保存および全件取得できる")
    void should_saveAndFindAll_when_productsPersisted() {
        // Arrange
        var p1 = createProduct("P007", "Ski Boots", "cat-3", "ACTIVE");
        var p2 = createProduct("P008", "Ski Poles", "cat-3", "ACTIVE");

        // Act
        productRepository.save(p1);
        productRepository.save(p2);
        entityManager.flush();
        entityManager.clear();
        var all = productRepository.findAll();

        // Assert
        assertThat(all).hasSizeGreaterThanOrEqualTo(2);
        assertThat(all).extracting(Product::getId).contains("P007", "P008");
    }

    @Test
    @DisplayName("商品IDで取得した場合、正しい商品を返す")
    void should_returnProduct_when_idExists() {
        // Arrange
        var product = createProduct("P009", "Helmet", "cat-4", "ACTIVE");
        entityManager.persistAndFlush(product);

        // Act
        var result = productRepository.findById("P009");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getName()).isEqualTo("Helmet");
        assertThat(result.orElseThrow().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("存在しないIDで取得した場合、空のOptionalを返す")
    void should_returnEmpty_when_productIdNotFound() {
        // Act
        var result = productRepository.findById("NONEXISTENT");

        // Assert
        assertThat(result).isEmpty();
    }
}
