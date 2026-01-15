package com.skishop.service.catalog;

import com.skishop.dao.product.ProductDao;
import com.skishop.dao.product.ProductDaoImpl;
import com.skishop.domain.product.Product;
import java.util.List;

public class ProductService {
  private final ProductDao productDao = new ProductDaoImpl();

  public Product findById(String productId) {
    return productDao.findById(productId);
  }

  public List<Product> search(String keyword, String categoryId, int offset, int limit) {
    return productDao.findPaged(keyword, categoryId, offset, limit);
  }
}
