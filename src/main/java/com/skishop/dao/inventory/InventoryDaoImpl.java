package com.skishop.dao.inventory;

import com.skishop.common.dao.AbstractDao;
import com.skishop.common.dao.DaoException;
import com.skishop.domain.inventory.Inventory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class InventoryDaoImpl extends AbstractDao implements InventoryDao {
  public Inventory findByProductId(String productId) {
    Connection con = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("SELECT id, product_id, quantity, reserved_quantity, status FROM inventory WHERE product_id = ?");
      ps.setString(1, productId);
      rs = ps.executeQuery();
      if (rs.next()) {
        Inventory inventory = new Inventory();
        inventory.setId(rs.getString("id"));
        inventory.setProductId(rs.getString("product_id"));
        inventory.setQuantity(rs.getInt("quantity"));
        inventory.setReservedQuantity(rs.getInt("reserved_quantity"));
        inventory.setStatus(rs.getString("status"));
        return inventory;
      }
      return null;
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(rs, ps, con);
    }
  }

  public boolean reserve(String productId, int quantity) {
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("UPDATE inventory SET reserved_quantity = reserved_quantity + ? WHERE product_id = ? AND (quantity - reserved_quantity) >= ?");
      ps.setInt(1, quantity);
      ps.setString(2, productId);
      ps.setInt(3, quantity);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(null, ps, con);
    }
  }
}
