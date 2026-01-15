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
      ps = con.prepareStatement("SELECT id, name, brand, description, category_id, sku, status, created_at, updated_at FROM products WHERE id = ?");
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

  public List findPaged(String keyword, String categoryId, int offset, int limit) {
    Connection con = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    List products = new ArrayList();
    try {
      StringBuffer sql = new StringBuffer();
      sql.append("SELECT id, name, brand, description, category_id, sku, status, created_at, updated_at FROM products WHERE 1=1");
      if (keyword != null && keyword.trim().length() > 0) {
        sql.append(" AND name LIKE ?");
      }
      if (categoryId != null && categoryId.trim().length() > 0) {
        sql.append(" AND category_id = ?");
      }
      sql.append(" ORDER BY name LIMIT ? OFFSET ?");

      con = getConnection();
      ps = con.prepareStatement(sql.toString());
      int index = 1;
      if (keyword != null && keyword.trim().length() > 0) {
        ps.setString(index++, "%" + keyword + "%");
      }
      if (categoryId != null && categoryId.trim().length() > 0) {
        ps.setString(index++, categoryId);
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
    return product;
  }
}
