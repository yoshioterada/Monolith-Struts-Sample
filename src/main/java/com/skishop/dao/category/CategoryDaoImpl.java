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

public class CategoryDaoImpl extends AbstractDao implements CategoryDao {
  public List<Category> findAll() {
    Connection con = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    List<Category> categories = new ArrayList<Category>();
    try {
      con = getConnection();
      ps = con.prepareStatement("SELECT id, name, parent_id FROM categories ORDER BY name");
      rs = ps.executeQuery();
      while (rs.next()) {
        Category c = new Category();
        c.setId(rs.getString("id"));
        c.setName(rs.getString("name"));
        c.setParentId(rs.getString("parent_id"));
        categories.add(c);
      }
      return categories;
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(rs, ps, con);
    }
  }
}
