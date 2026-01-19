package com.skishop.dao.product;

import com.skishop.common.dao.AbstractDao;
import com.skishop.common.dao.DaoException;
import com.skishop.domain.product.Product;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ProductDaoImpl extends AbstractDao implements ProductDao {
  public Product findById(String id) {
    var sql = """
        SELECT p.id, p.name, p.brand, p.description, p.category_id, p.sku, p.status, p.created_at, p.updated_at, pr.regular_price, pr.sale_price, pr.currency_code
        FROM products p LEFT JOIN prices pr ON pr.product_id = p.id WHERE p.id = ?
        """;
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, id);
      try (var rs = ps.executeQuery()) {
        if (rs.next()) {
          return mapProduct(rs);
        }
      }
      return null;
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public List<Product> findPaged(String keyword, String categoryId, String sort, int offset, int limit) {
    final var products = new ArrayList<Product>();
    var sql = new StringBuilder();
    sql.append("""
        SELECT p.id, p.name, p.brand, p.description, p.category_id, p.sku, p.status, p.created_at, p.updated_at, pr.regular_price, pr.sale_price, pr.currency_code
        FROM products p
        LEFT JOIN prices pr ON pr.product_id = p.id
        LEFT JOIN categories c ON c.id = p.category_id
        WHERE 1=1 AND p.status = 'ACTIVE'
        """);
    if (keyword != null && !keyword.isBlank()) {
      sql.append(" AND (p.name ILIKE ? OR p.brand ILIKE ? OR p.description ILIKE ?)");
    }
    if (categoryId != null && !categoryId.isBlank()) {
      sql.append(" AND (p.category_id = ? OR c.name ILIKE ?)");
    }
    if (sort != null) {
      sql.append(switch (sort) {
        case "priceAsc" -> " ORDER BY COALESCE(pr.sale_price, pr.regular_price, 999999999) ASC, p.name";
        case "priceDesc" -> " ORDER BY COALESCE(pr.sale_price, pr.regular_price, 0) DESC, p.name";
        case "newest" -> " ORDER BY p.created_at DESC";
        default -> " ORDER BY p.name";
      });
    } else {
      sql.append(" ORDER BY p.name");
    }
    sql.append(" LIMIT ? OFFSET ?");

    try (var con = getConnection(); var ps = con.prepareStatement(sql.toString())) {
      int index = 1;
      if (keyword != null && !keyword.isBlank()) {
        ps.setString(index++, "%" + keyword + "%");
        ps.setString(index++, "%" + keyword + "%");
        ps.setString(index++, "%" + keyword + "%");
      }
      if (categoryId != null && !categoryId.isBlank()) {
        ps.setString(index++, categoryId);
        ps.setString(index++, "%" + categoryId + "%");
      }
      ps.setInt(index++, limit);
      ps.setInt(index, offset);

      try (var rs = ps.executeQuery()) {
        while (rs.next()) {
          products.add(mapProduct(rs));
        }
      }
      return products;
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public void insert(Product product) {
    var sql = "INSERT INTO products(id, name, brand, description, category_id, sku, status, created_at, updated_at) VALUES(?,?,?,?,?,?,?,?,?)";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, product.getId());
      ps.setString(2, product.getName());
      ps.setString(3, product.getBrand());
      ps.setString(4, product.getDescription());
      ps.setString(5, product.getCategoryId());
      ps.setString(6, product.getSku());
      ps.setString(7, product.getStatus());
      var now = new java.sql.Timestamp(System.currentTimeMillis());
      ps.setTimestamp(8, product.getCreatedAt() != null ? new java.sql.Timestamp(product.getCreatedAt().getTime()) : now);
      ps.setTimestamp(9, product.getUpdatedAt() != null ? new java.sql.Timestamp(product.getUpdatedAt().getTime()) : now);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public void update(Product product) {
    var sql = "UPDATE products SET name = ?, brand = ?, description = ?, category_id = ?, sku = ?, status = ?, updated_at = ? WHERE id = ?";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, product.getName());
      ps.setString(2, product.getBrand());
      ps.setString(3, product.getDescription());
      ps.setString(4, product.getCategoryId());
      ps.setString(5, product.getSku());
      ps.setString(6, product.getStatus());
      ps.setTimestamp(7, new java.sql.Timestamp(System.currentTimeMillis()));
      ps.setString(8, product.getId());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  private Product mapProduct(ResultSet rs) throws SQLException {
    Product product = new Product();
    product.setId(rs.getString("id"));
    product.setName(rs.getString("name"));
    product.setBrand(rs.getString("brand"));
    product.setDescription(rs.getString("description"));
    product.setCategoryId(rs.getString("category_id"));
    product.setSku(rs.getString("sku"));
    product.setStatus(rs.getString("status"));
    product.setCreatedAt(rs.getTimestamp("created_at"));
    product.setUpdatedAt(rs.getTimestamp("updated_at"));
    try { product.setRegularPrice(rs.getBigDecimal("regular_price")); } catch (SQLException ignore) {}
    try { product.setSalePrice(rs.getBigDecimal("sale_price")); } catch (SQLException ignore) {}
    try { product.setCurrencyCode(rs.getString("currency_code")); } catch (SQLException ignore) {}
    return product;
  }
}
