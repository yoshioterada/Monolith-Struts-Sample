package com.skishop.dao.category;

import com.skishop.domain.product.Category;
import java.util.List;

public interface CategoryDao {
  List<Category> findAll();
}
