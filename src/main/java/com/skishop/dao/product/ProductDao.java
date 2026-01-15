package com.skishop.dao.product;

import com.skishop.domain.product.Product;
import java.util.List;

public interface ProductDao {
  Product findById(String id);

  List findPaged(String keyword, String categoryId, int offset, int limit);
}
