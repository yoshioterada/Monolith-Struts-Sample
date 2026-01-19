package com.skishop.dao.cart;

import com.skishop.common.dao.AbstractDao;
import com.skishop.common.dao.DaoException;
import com.skishop.domain.cart.Cart;
import com.skishop.domain.cart.CartItem;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class CartDaoImpl extends AbstractDao implements CartDao {
  public Cart findById(String id) {
    var sql = "SELECT id, user_id, session_id, status, expires_at FROM carts WHERE id = ?";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, id);
      try (var rs = ps.executeQuery()) {
        if (rs.next()) {
          var cart = new Cart();
          cart.setId(rs.getString("id"));
          cart.setUserId(rs.getString("user_id"));
          cart.setSessionId(rs.getString("session_id"));
          cart.setStatus(rs.getString("status"));
          cart.setExpiresAt(rs.getTimestamp("expires_at"));
          return cart;
        }
      }
      return null;
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public void insert(Cart cart) {
    var sql = "INSERT INTO carts(id, user_id, session_id, status, expires_at) VALUES(?,?,?,?,?)";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, cart.getId());
      ps.setString(2, cart.getUserId());
      ps.setString(3, cart.getSessionId());
      ps.setString(4, cart.getStatus());
      ps.setTimestamp(5, toTimestamp(cart.getExpiresAt()));
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public void addItem(CartItem item) {
    var sql = "INSERT INTO cart_items(id, cart_id, product_id, quantity, unit_price) VALUES(?,?,?,?,?)";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, item.getId());
      ps.setString(2, item.getCartId());
      ps.setString(3, item.getProductId());
      ps.setInt(4, item.getQuantity());
      ps.setBigDecimal(5, item.getUnitPrice());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public List<CartItem> findItemsByCartId(String cartId) {
    var sql = """
        SELECT ci.id, ci.cart_id, ci.product_id, ci.quantity, ci.unit_price, p.name AS product_name
        FROM cart_items ci LEFT JOIN products p ON ci.product_id = p.id
        WHERE ci.cart_id = ?
        ORDER BY ci.id
        """;
    final var items = new ArrayList<CartItem>();
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, cartId);
      try (var rs = ps.executeQuery()) {
        while (rs.next()) {
          var item = new CartItem();
          item.setId(rs.getString("id"));
          item.setCartId(rs.getString("cart_id"));
          item.setProductId(rs.getString("product_id"));
          item.setProductName(rs.getString("product_name"));
          item.setQuantity(rs.getInt("quantity"));
          item.setUnitPrice(rs.getBigDecimal("unit_price"));
          items.add(item);
        }
      }
      return items;
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public void updateItemQuantity(String itemId, int quantity) {
    var sql = "UPDATE cart_items SET quantity = ? WHERE id = ?";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setInt(1, quantity);
      ps.setString(2, itemId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public void deleteItemsByCartId(String cartId) {
    var sql = "DELETE FROM cart_items WHERE cart_id = ?";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, cartId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public void updateStatus(String cartId, String status) {
    var sql = "UPDATE carts SET status = ? WHERE id = ?";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, status);
      ps.setString(2, cartId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  private Timestamp toTimestamp(java.util.Date date) {
    if (date == null) {
      return null;
    }
    return new Timestamp(date.getTime());
  }
}
