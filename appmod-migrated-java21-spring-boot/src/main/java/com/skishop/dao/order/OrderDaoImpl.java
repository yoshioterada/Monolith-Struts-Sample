package com.skishop.dao.order;

import com.skishop.common.dao.AbstractDao;
import com.skishop.common.dao.DaoException;
import com.skishop.domain.order.Order;
import com.skishop.domain.order.OrderItem;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class OrderDaoImpl extends AbstractDao implements OrderDao {
  public Order findById(String id) {
    var sql = "SELECT id, order_number, user_id, status, payment_status, subtotal, tax, shipping_fee, discount_amount, total_amount, coupon_code, used_points, created_at, updated_at FROM orders WHERE id = ?";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, id);
      try (var rs = ps.executeQuery()) {
        if (rs.next()) {
          return mapOrder(rs);
        }
      }
      return null;
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public List<Order> listByUserId(String userId) {
    var sql = "SELECT id, order_number, user_id, status, payment_status, subtotal, tax, shipping_fee, discount_amount, total_amount, coupon_code, used_points, created_at, updated_at FROM orders WHERE user_id = ? ORDER BY created_at DESC";
    final var orders = new ArrayList<Order>();
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, userId);
      try (var rs = ps.executeQuery()) {
        while (rs.next()) {
          orders.add(mapOrder(rs));
        }
      }
      return orders;
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public List<Order> listAll(int limit) {
    var sql = "SELECT id, order_number, user_id, status, payment_status, subtotal, tax, shipping_fee, discount_amount, total_amount, coupon_code, used_points, created_at, updated_at FROM orders ORDER BY created_at DESC LIMIT ?";
    final var orders = new ArrayList<Order>();
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setInt(1, limit);
      try (var rs = ps.executeQuery()) {
        while (rs.next()) {
          orders.add(mapOrder(rs));
        }
      }
      return orders;
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public void insertOrder(Order order) {
    var sql = "INSERT INTO orders(id, order_number, user_id, status, payment_status, subtotal, tax, shipping_fee, discount_amount, total_amount, coupon_code, used_points, created_at, updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, order.getId());
      ps.setString(2, order.getOrderNumber());
      ps.setString(3, order.getUserId());
      ps.setString(4, order.getStatus());
      ps.setString(5, order.getPaymentStatus());
      ps.setBigDecimal(6, order.getSubtotal());
      ps.setBigDecimal(7, order.getTax());
      ps.setBigDecimal(8, order.getShippingFee());
      ps.setBigDecimal(9, order.getDiscountAmount());
      ps.setBigDecimal(10, order.getTotalAmount());
      ps.setString(11, order.getCouponCode());
      ps.setInt(12, order.getUsedPoints());
      ps.setTimestamp(13, toTimestamp(order.getCreatedAt()));
      ps.setTimestamp(14, toTimestamp(order.getUpdatedAt()));
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public void insertOrderItem(OrderItem item) {
    var sql = "INSERT INTO order_items(id, order_id, product_id, product_name, sku, unit_price, quantity, subtotal) VALUES(?,?,?,?,?,?,?,?)";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, item.getId());
      ps.setString(2, item.getOrderId());
      ps.setString(3, item.getProductId());
      ps.setString(4, item.getProductName());
      ps.setString(5, item.getSku());
      ps.setBigDecimal(6, item.getUnitPrice());
      ps.setInt(7, item.getQuantity());
      ps.setBigDecimal(8, item.getSubtotal());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public List<OrderItem> listItemsByOrderId(String orderId) {
    var sql = "SELECT id, order_id, product_id, product_name, sku, unit_price, quantity, subtotal FROM order_items WHERE order_id = ? ORDER BY id";
    final var items = new ArrayList<OrderItem>();
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, orderId);
      try (var rs = ps.executeQuery()) {
        while (rs.next()) {
          var item = new OrderItem();
          item.setId(rs.getString("id"));
          item.setOrderId(rs.getString("order_id"));
          item.setProductId(rs.getString("product_id"));
          item.setProductName(rs.getString("product_name"));
          item.setSku(rs.getString("sku"));
          item.setUnitPrice(rs.getBigDecimal("unit_price"));
          item.setQuantity(rs.getInt("quantity"));
          item.setSubtotal(rs.getBigDecimal("subtotal"));
          items.add(item);
        }
      }
      return items;
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public void updateStatus(String orderId, String status) {
    var sql = "UPDATE orders SET status = ?, updated_at = ? WHERE id = ?";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, status);
      ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
      ps.setString(3, orderId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public void updatePaymentStatus(String orderId, String paymentStatus) {
    var sql = "UPDATE orders SET payment_status = ?, updated_at = ? WHERE id = ?";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, paymentStatus);
      ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
      ps.setString(3, orderId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  private Order mapOrder(ResultSet rs) throws SQLException {
    Order order = new Order();
    order.setId(rs.getString("id"));
    order.setOrderNumber(rs.getString("order_number"));
    order.setUserId(rs.getString("user_id"));
    order.setStatus(rs.getString("status"));
    order.setPaymentStatus(rs.getString("payment_status"));
    order.setSubtotal(rs.getBigDecimal("subtotal"));
    order.setTax(rs.getBigDecimal("tax"));
    order.setShippingFee(rs.getBigDecimal("shipping_fee"));
    order.setDiscountAmount(rs.getBigDecimal("discount_amount"));
    order.setTotalAmount(rs.getBigDecimal("total_amount"));
    order.setCouponCode(rs.getString("coupon_code"));
    order.setUsedPoints(rs.getInt("used_points"));
    order.setCreatedAt(rs.getTimestamp("created_at"));
    order.setUpdatedAt(rs.getTimestamp("updated_at"));
    return order;
  }

  private Timestamp toTimestamp(java.util.Date date) {
    if (date == null) {
      return null;
    }
    return new Timestamp(date.getTime());
  }
}
