package com.skishop.dao.order;

import com.skishop.common.dao.AbstractDao;
import com.skishop.common.dao.DaoException;
import com.skishop.domain.order.Return;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReturnDaoImpl extends AbstractDao implements ReturnDao {
  public void insert(Return returnItem) {
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("INSERT INTO returns(id, order_id, order_item_id, reason, quantity, refund_amount, status) VALUES(?,?,?,?,?,?,?)");
      ps.setString(1, returnItem.getId());
      ps.setString(2, returnItem.getOrderId());
      ps.setString(3, returnItem.getOrderItemId());
      ps.setString(4, returnItem.getReason());
      ps.setInt(5, returnItem.getQuantity());
      ps.setBigDecimal(6, returnItem.getRefundAmount());
      ps.setString(7, returnItem.getStatus());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(null, ps, con);
    }
  }

  public List<Return> listByOrderId(String orderId) {
    Connection con = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    List<Return> returns = new ArrayList<Return>();
    try {
      con = getConnection();
      ps = con.prepareStatement("SELECT id, order_id, order_item_id, reason, quantity, refund_amount, status FROM returns WHERE order_id = ? ORDER BY id");
      ps.setString(1, orderId);
      rs = ps.executeQuery();
      while (rs.next()) {
        Return returnItem = new Return();
        returnItem.setId(rs.getString("id"));
        returnItem.setOrderId(rs.getString("order_id"));
        returnItem.setOrderItemId(rs.getString("order_item_id"));
        returnItem.setReason(rs.getString("reason"));
        returnItem.setQuantity(rs.getInt("quantity"));
        returnItem.setRefundAmount(rs.getBigDecimal("refund_amount"));
        returnItem.setStatus(rs.getString("status"));
        returns.add(returnItem);
      }
      return returns;
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(rs, ps, con);
    }
  }
}
