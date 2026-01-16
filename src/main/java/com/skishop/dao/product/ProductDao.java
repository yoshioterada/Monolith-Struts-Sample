package com.skishop.dao.product;

import com.skishop.domain.product.Product;
import java.util.List;

public interface ProductDao {
  Product findById(String id);

  List<Product> findPaged(String keyword, String categoryId, String sort, int offset, int limit);

  void insert(Product product);

  void update(Product product);
}
