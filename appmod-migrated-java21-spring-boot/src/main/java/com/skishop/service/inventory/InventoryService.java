package com.skishop.service.inventory;

import com.skishop.common.dao.DataSourceLocator;
import com.skishop.domain.cart.CartItem;
import com.skishop.domain.inventory.Inventory;
import org.springframework.stereotype.Service;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class InventoryService {
  public void reserveItems(List<CartItem> items) {
    updateReservation(items, true);
  }

  public void releaseItems(List<CartItem> items) {
    updateReservation(items, false);
  }

  private void updateReservation(List<CartItem> items, boolean reserve) {
    if (items == null || items.isEmpty()) {
      return;
    }
    Connection con = null;
    try {
      con = DataSourceLocator.getInstance().getDataSource().getConnection();
      con.setAutoCommit(false);
      for (CartItem item : items) {
        Inventory inventory = lockInventory(con, item.getProductId());
        if (inventory == null) {
          throw new IllegalStateException("Inventory not found: " + item.getProductId());
        }
        if (reserve) {
          int available = inventory.getQuantity() - inventory.getReservedQuantity();
          if (available < item.getQuantity()) {
            throw new IllegalStateException("Insufficient stock: " + item.getProductId());
          }
          updateReservedQuantity(con, item.getProductId(), item.getQuantity());
        } else {
          updateReservedQuantity(con, item.getProductId(), -item.getQuantity());
        }
      }
      con.commit();
    } catch (SQLException e) {
      rollbackQuietly(con);
      throw new IllegalStateException("Inventory update failed", e);
    } catch (RuntimeException e) {
      rollbackQuietly(con);
      throw e;
    } finally {
      closeQuietly(con);
    }
  }

  private Inventory lockInventory(Connection con, String productId) throws SQLException {
    var sql = "SELECT id, product_id, quantity, reserved_quantity, status FROM inventory WHERE product_id = ? FOR UPDATE";
    try (var ps = con.prepareStatement(sql)) {
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
    }
  }

  private void updateReservedQuantity(Connection con, String productId, int delta) throws SQLException {
    var sql = "UPDATE inventory SET reserved_quantity = CASE WHEN reserved_quantity + ? < 0 THEN 0 ELSE reserved_quantity + ? END WHERE product_id = ?";
    try (var ps = con.prepareStatement(sql)) {
      ps.setInt(1, delta);
      ps.setInt(2, delta);
      ps.setString(3, productId);
      ps.executeUpdate();
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
        // ignore cleanup errors
      }
    }
  }
}
