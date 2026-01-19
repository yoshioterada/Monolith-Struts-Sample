package com.skishop.dao.category;

import com.skishop.common.dao.AbstractDao;
import com.skishop.common.dao.DaoException;
import com.skishop.domain.product.Category;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class CategoryDaoImpl extends AbstractDao implements CategoryDao {
  public List<Category> findAll() {
    var sql = "SELECT id, name, parent_id FROM categories ORDER BY name";
    final var categories = new ArrayList<Category>();
    try (var con = getConnection(); var ps = con.prepareStatement(sql); var rs = ps.executeQuery()) {
      while (rs.next()) {
        var c = new Category();
        c.setId(rs.getString("id"));
        c.setName(rs.getString("name"));
        c.setParentId(rs.getString("parent_id"));
        categories.add(c);
      }
      return categories;
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }
}
