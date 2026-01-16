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

public class ProductDaoImpl extends AbstractDao implements ProductDao {
  public Product findById(String id) {
    Connection con = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      con = getConnection();
        ps = con.prepareStatement("SELECT p.id, p.name, p.brand, p.description, p.category_id, p.sku, p.status, p.created_at, p.updated_at, pr.regular_price, pr.sale_price, pr.currency_code "
          + "FROM products p LEFT JOIN prices pr ON pr.product_id = p.id WHERE p.id = ?");
      ps.setString(1, id);
      rs = ps.executeQuery();
      if (rs.next()) {
        return mapProduct(rs);
      }
      return null;
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(rs, ps, con);
    }
  }

  public List<Product> findPaged(String keyword, String categoryId, String sort, int offset, int limit) {
    Connection con = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    List<Product> products = new ArrayList<Product>();
    try {
      StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.id, p.name, p.brand, p.description, p.category_id, p.sku, p.status, p.created_at, p.updated_at, pr.regular_price, pr.sale_price, pr.currency_code "
          + "FROM products p "
          + "LEFT JOIN prices pr ON pr.product_id = p.id "
          + "LEFT JOIN categories c ON c.id = p.category_id "
          + "WHERE 1=1 AND p.status = 'ACTIVE'");
      if (keyword != null && keyword.trim().length() > 0) {
        sql.append(" AND (p.name ILIKE ? OR p.brand ILIKE ? OR p.description ILIKE ?)");
      }
      if (categoryId != null && categoryId.trim().length() > 0) {
        sql.append(" AND (p.category_id = ? OR c.name ILIKE ?)");
      }
      if (sort != null) {
        if ("priceAsc".equals(sort)) {
          sql.append(" ORDER BY COALESCE(pr.sale_price, pr.regular_price, 999999999) ASC, p.name");
        } else if ("priceDesc".equals(sort)) {
          sql.append(" ORDER BY COALESCE(pr.sale_price, pr.regular_price, 0) DESC, p.name");
        } else if ("newest".equals(sort)) {
          sql.append(" ORDER BY p.created_at DESC");
        } else {
          sql.append(" ORDER BY p.name");
        }
      } else {
        sql.append(" ORDER BY p.name");
      }
      sql.append(" LIMIT ? OFFSET ?");

      con = getConnection();
      ps = con.prepareStatement(sql.toString());
      int index = 1;
      if (keyword != null && keyword.trim().length() > 0) {
        ps.setString(index++, "%" + keyword + "%");
        ps.setString(index++, "%" + keyword + "%");
        ps.setString(index++, "%" + keyword + "%");
      }
      if (categoryId != null && categoryId.trim().length() > 0) {
        ps.setString(index++, categoryId);
        ps.setString(index++, "%" + categoryId + "%");
      }
      ps.setInt(index++, limit);
      ps.setInt(index, offset);

      rs = ps.executeQuery();
      while (rs.next()) {
        products.add(mapProduct(rs));
      }
      return products;
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(rs, ps, con);
    }
  }

  public void insert(Product product) {
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("INSERT INTO products(id, name, brand, description, category_id, sku, status, created_at, updated_at) VALUES(?,?,?,?,?,?,?,?,?)");
      ps.setString(1, product.getId());
      ps.setString(2, product.getName());
      ps.setString(3, product.getBrand());
      ps.setString(4, product.getDescription());
      ps.setString(5, product.getCategoryId());
      ps.setString(6, product.getSku());
      ps.setString(7, product.getStatus());
      java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
      ps.setTimestamp(8, product.getCreatedAt() != null ? new java.sql.Timestamp(product.getCreatedAt().getTime()) : now);
      ps.setTimestamp(9, product.getUpdatedAt() != null ? new java.sql.Timestamp(product.getUpdatedAt().getTime()) : now);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(null, ps, con);
    }
  }

  public void update(Product product) {
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("UPDATE products SET name = ?, brand = ?, description = ?, category_id = ?, sku = ?, status = ?, updated_at = ? WHERE id = ?");
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
    } finally {
      closeQuietly(null, ps, con);
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
