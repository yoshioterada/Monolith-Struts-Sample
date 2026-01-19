package com.skishop.dao.inventory;

import com.skishop.common.dao.AbstractDao;
import com.skishop.common.dao.DaoException;
import com.skishop.domain.inventory.Inventory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.stereotype.Repository;

@Repository
public class InventoryDaoImpl extends AbstractDao implements InventoryDao {
  public Inventory findByProductId(String productId) {
    var sql = "SELECT id, product_id, quantity, reserved_quantity, status FROM inventory WHERE product_id = ?";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, productId);
      try (var rs = ps.executeQuery()) {
        if (rs.next()) {
          var inventory = new Inventory();
          inventory.setId(rs.getString("id"));
          inventory.setProductId(rs.getString("product_id"));
          inventory.setQuantity(rs.getInt("quantity"));
          inventory.setReservedQuantity(rs.getInt("reserved_quantity"));
          inventory.setStatus(rs.getString("status"));
          return inventory;
        }
      }
      return null;
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public void insert(Inventory inventory) {
    var sql = "INSERT INTO inventory(id, product_id, quantity, reserved_quantity, status) VALUES(?,?,?,?,?)";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, inventory.getId());
      ps.setString(2, inventory.getProductId());
      ps.setInt(3, inventory.getQuantity());
      ps.setInt(4, inventory.getReservedQuantity());
      ps.setString(5, inventory.getStatus());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public void updateQuantity(String productId, int quantity, String status) {
    var sql = "UPDATE inventory SET quantity = ?, status = ? WHERE product_id = ?";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setInt(1, quantity);
      ps.setString(2, status);
      ps.setString(3, productId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public boolean reserve(String productId, int quantity) {
    var sql = "UPDATE inventory SET reserved_quantity = reserved_quantity + ? WHERE product_id = ? AND (quantity - reserved_quantity) >= ?";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setInt(1, quantity);
      ps.setString(2, productId);
      ps.setInt(3, quantity);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }
}
