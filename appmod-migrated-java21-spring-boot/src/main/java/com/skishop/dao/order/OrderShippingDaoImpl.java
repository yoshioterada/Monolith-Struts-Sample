package com.skishop.dao.order;

import com.skishop.common.dao.AbstractDao;
import com.skishop.common.dao.DaoException;
import com.skishop.domain.order.OrderShipping;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.springframework.stereotype.Repository;

@Repository
public class OrderShippingDaoImpl extends AbstractDao implements OrderShippingDao {
  public void insert(OrderShipping shipping) {
    var sql = "INSERT INTO order_shipping(id, order_id, recipient_name, postal_code, prefecture, address1, address2, phone, shipping_method_code, shipping_fee, requested_delivery_date) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, shipping.getId());
      ps.setString(2, shipping.getOrderId());
      ps.setString(3, shipping.getRecipientName());
      ps.setString(4, shipping.getPostalCode());
      ps.setString(5, shipping.getPrefecture());
      ps.setString(6, shipping.getAddress1());
      ps.setString(7, shipping.getAddress2());
      ps.setString(8, shipping.getPhone());
      ps.setString(9, shipping.getShippingMethodCode());
      ps.setBigDecimal(10, shipping.getShippingFee());
      ps.setTimestamp(11, toTimestamp(shipping.getRequestedDeliveryDate()));
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public OrderShipping findByOrderId(String orderId) {
    var sql = "SELECT id, order_id, recipient_name, postal_code, prefecture, address1, address2, phone, shipping_method_code, shipping_fee, requested_delivery_date FROM order_shipping WHERE order_id = ?";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, orderId);
      try (var rs = ps.executeQuery()) {
        if (rs.next()) {
          var shipping = new OrderShipping();
          shipping.setId(rs.getString("id"));
          shipping.setOrderId(rs.getString("order_id"));
          shipping.setRecipientName(rs.getString("recipient_name"));
          shipping.setPostalCode(rs.getString("postal_code"));
          shipping.setPrefecture(rs.getString("prefecture"));
          shipping.setAddress1(rs.getString("address1"));
          shipping.setAddress2(rs.getString("address2"));
          shipping.setPhone(rs.getString("phone"));
          shipping.setShippingMethodCode(rs.getString("shipping_method_code"));
          shipping.setShippingFee(rs.getBigDecimal("shipping_fee"));
          shipping.setRequestedDeliveryDate(rs.getTimestamp("requested_delivery_date"));
          return shipping;
        }
      }
      return null;
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
