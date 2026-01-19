package com.skishop.service.catalog;

import com.skishop.common.dao.DataSourceLocator;
import com.skishop.dao.product.ProductDao;
import com.skishop.domain.product.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Service
public class ProductService {
  private static final String STATUS_INACTIVE = "INACTIVE";
  private static final String STATUS_OUT_OF_STOCK = "OUT_OF_STOCK";
  private final ProductDao productDao;

  @Autowired
  public ProductService(ProductDao productDao) {
    this.productDao = productDao;
  }

  public Product findById(String productId) {
    return productDao.findById(productId);
  }

  public List<Product> search(String keyword, String categoryId, String sort, int offset, int limit) {
    return productDao.findPaged(keyword, categoryId, sort, offset, limit);
  }

  public List<Product> search(String keyword, String categoryId, int offset, int limit) {
    return productDao.findPaged(keyword, categoryId, null, offset, limit);
  }

  public void deactivateProduct(String productId) {
    Connection con = null;
    try {
      con = DataSourceLocator.getInstance().getDataSource().getConnection();
      con.setAutoCommit(false);
      try (var psProduct = con.prepareStatement("UPDATE products SET status = ?, updated_at = ? WHERE id = ?")) {
        psProduct.setString(1, STATUS_INACTIVE);
        psProduct.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
        psProduct.setString(3, productId);
        psProduct.executeUpdate();
      }
      try (var psInventory = con.prepareStatement("UPDATE inventory SET quantity = ?, status = ? WHERE product_id = ?")) {
        psInventory.setInt(1, 0);
        psInventory.setString(2, STATUS_OUT_OF_STOCK);
        psInventory.setString(3, productId);
        psInventory.executeUpdate();
      }
      con.commit();
    } catch (SQLException e) {
      rollbackQuietly(con);
      throw new IllegalStateException("Failed to deactivate product", e);
    } finally {
      closeQuietly(con);
    }
  }

  private void rollbackQuietly(Connection con) {
    if (con != null) {
      try {
        con.rollback();
      } catch (SQLException e) {
        // ignore rollback failures
      }
    }
  }

  private void closeQuietly(Connection con) {
    if (con != null) {
      try {
        con.close();
      } catch (SQLException e) {
        // ignore close errors
      }
    }
  }

  private void closeQuietly(PreparedStatement statement) {
    if (statement != null) {
      try {
        statement.close();
      } catch (SQLException e) {
        // ignore close errors
      }
    }
  }
}
