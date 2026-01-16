package com.skishop.service.catalog;

import com.skishop.dao.category.CategoryDao;
import com.skishop.dao.category.CategoryDaoImpl;
import com.skishop.domain.product.Category;
import java.util.List;

public class CategoryService {
  private final CategoryDao categoryDao = new CategoryDaoImpl();

  public List<Category> listAll() {
    return categoryDao.findAll();
  }
}
