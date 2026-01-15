package com.skishop.dao;

import com.skishop.dao.product.ProductDao;
import com.skishop.dao.product.ProductDaoImpl;
import com.skishop.domain.product.Product;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ProductDaoTest extends DaoTestBase {
  private ProductDao productDao;

  @Before
  public void setUp() throws Exception {
    resetDatabase();
    productDao = new ProductDaoImpl();
  }

  @Test
  public void testFindById() {
    Product product = productDao.findById("P001");
    Assert.assertNotNull(product);
    Assert.assertEquals("Ski A", product.getName());
  }

  @Test
  public void testFindPaged() {
    List products = productDao.findPaged("Ski", "c-1", 0, 10);
    Assert.assertFalse(products.isEmpty());
  }
}
