package com.skishop.dao.payment;

import com.skishop.common.dao.AbstractDao;
import com.skishop.common.dao.DaoException;
import com.skishop.domain.payment.Payment;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class PaymentDaoImpl extends AbstractDao implements PaymentDao {
  public void insert(Payment payment) {
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("INSERT INTO payments(id, order_id, cart_id, amount, currency, status, payment_intent_id, created_at) VALUES(?,?,?,?,?,?,?,?)");
      ps.setString(1, payment.getId());
      ps.setString(2, payment.getOrderId());
      ps.setString(3, payment.getCartId());
      ps.setBigDecimal(4, payment.getAmount());
      ps.setString(5, payment.getCurrency());
      ps.setString(6, payment.getStatus());
      ps.setString(7, payment.getPaymentIntentId());
      ps.setTimestamp(8, toTimestamp(payment.getCreatedAt()));
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(null, ps, con);
    }
  }

  public Payment findByOrderId(String orderId) {
    Connection con = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("SELECT id, order_id, cart_id, amount, currency, status, payment_intent_id, created_at FROM payments WHERE order_id = ?");
      ps.setString(1, orderId);
      rs = ps.executeQuery();
      if (rs.next()) {
        Payment payment = new Payment();
        payment.setId(rs.getString("id"));
        payment.setOrderId(rs.getString("order_id"));
        payment.setCartId(rs.getString("cart_id"));
        payment.setAmount(rs.getBigDecimal("amount"));
        payment.setCurrency(rs.getString("currency"));
        payment.setStatus(rs.getString("status"));
        payment.setPaymentIntentId(rs.getString("payment_intent_id"));
        payment.setCreatedAt(rs.getTimestamp("created_at"));
        return payment;
      }
      return null;
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(rs, ps, con);
    }
  }

  public void updateStatus(String paymentId, String status) {
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("UPDATE payments SET status = ? WHERE id = ?");
      ps.setString(1, status);
      ps.setString(2, paymentId);
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
