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

public class CartDaoImpl extends AbstractDao implements CartDao {
  public Cart findById(String id) {
    Connection con = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("SELECT id, user_id, session_id, status, expires_at FROM carts WHERE id = ?");
      ps.setString(1, id);
      rs = ps.executeQuery();
      if (rs.next()) {
        Cart cart = new Cart();
        cart.setId(rs.getString("id"));
        cart.setUserId(rs.getString("user_id"));
        cart.setSessionId(rs.getString("session_id"));
        cart.setStatus(rs.getString("status"));
        cart.setExpiresAt(rs.getTimestamp("expires_at"));
        return cart;
      }
      return null;
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(rs, ps, con);
    }
  }

  public void insert(Cart cart) {
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("INSERT INTO carts(id, user_id, session_id, status, expires_at) VALUES(?,?,?,?,?)");
      ps.setString(1, cart.getId());
      ps.setString(2, cart.getUserId());
      ps.setString(3, cart.getSessionId());
      ps.setString(4, cart.getStatus());
      ps.setTimestamp(5, toTimestamp(cart.getExpiresAt()));
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(null, ps, con);
    }
  }

  public void addItem(CartItem item) {
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("INSERT INTO cart_items(id, cart_id, product_id, quantity, unit_price) VALUES(?,?,?,?,?)");
      ps.setString(1, item.getId());
      ps.setString(2, item.getCartId());
      ps.setString(3, item.getProductId());
      ps.setInt(4, item.getQuantity());
      ps.setBigDecimal(5, item.getUnitPrice());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(null, ps, con);
    }
  }

  public List findItemsByCartId(String cartId) {
    Connection con = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    List items = new ArrayList();
    try {
      con = getConnection();
      ps = con.prepareStatement("SELECT id, cart_id, product_id, quantity, unit_price FROM cart_items WHERE cart_id = ? ORDER BY id");
      ps.setString(1, cartId);
      rs = ps.executeQuery();
      while (rs.next()) {
        CartItem item = new CartItem();
        item.setId(rs.getString("id"));
        item.setCartId(rs.getString("cart_id"));
        item.setProductId(rs.getString("product_id"));
        item.setQuantity(rs.getInt("quantity"));
        item.setUnitPrice(rs.getBigDecimal("unit_price"));
        items.add(item);
      }
      return items;
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(rs, ps, con);
    }
  }

  public void updateItemQuantity(String itemId, int quantity) {
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("UPDATE cart_items SET quantity = ? WHERE id = ?");
      ps.setInt(1, quantity);
      ps.setString(2, itemId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(null, ps, con);
    }
  }

  private Timestamp toTimestamp(java.util.Date date) {
    if (date == null) {
      return null;
    }
    return new Timestamp(date.getTime());
  }
}
