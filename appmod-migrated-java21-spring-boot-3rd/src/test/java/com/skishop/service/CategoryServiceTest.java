package com.skishop.service;

import com.skishop.model.Category;
import com.skishop.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    @DisplayName("全カテゴリを取得した場合、全件返す")
    void should_returnAllCategories_when_listAll() {
        // Arrange
        var cat1 = new Category();
        cat1.setId("cat-1");
        var cat2 = new Category();
        cat2.setId("cat-2");
        when(categoryRepository.findAll()).thenReturn(List.of(cat1, cat2));

        // Act
        var result = categoryService.listAll();

        // Assert
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("ルートカテゴリを取得した場合、parentがnullのカテゴリを返す")
    void should_returnRootCategories_when_listRootCategories() {
        // Arrange
        var rootCat = new Category();
        rootCat.setId("root-1");
        when(categoryRepository.findByParentIsNull()).thenReturn(List.of(rootCat));

        // Act
        var result = categoryService.listRootCategories();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("root-1");
    }

    @Test
    @DisplayName("親IDでカテゴリを検索した場合、子カテゴリを返す")
    void should_returnChildren_when_listByParentId() {
        // Arrange
        var child = new Category();
        child.setId("child-1");
        when(categoryRepository.findByParentId("parent-1")).thenReturn(List.of(child));

        // Act
        var result = categoryService.listByParentId("parent-1");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("child-1");
    }

    @Test
    @DisplayName("カテゴリが存在しない場合、空のリストを返す")
    void should_returnEmptyList_when_noCategories() {
        // Arrange
        when(categoryRepository.findAll()).thenReturn(List.of());

        // Act
        var result = categoryService.listAll();

        // Assert
        assertThat(result).isEmpty();
    }
}
