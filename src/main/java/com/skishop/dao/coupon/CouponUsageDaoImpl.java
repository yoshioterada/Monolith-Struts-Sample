package com.skishop.dao.coupon;

import com.skishop.common.dao.AbstractDao;
import com.skishop.common.dao.DaoException;
import com.skishop.domain.coupon.CouponUsage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class CouponUsageDaoImpl extends AbstractDao implements CouponUsageDao {
  public void insert(CouponUsage usage) {
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("INSERT INTO coupon_usage(id, coupon_id, user_id, order_id, discount_applied, used_at) VALUES(?,?,?,?,?,?)");
      ps.setString(1, usage.getId());
      ps.setString(2, usage.getCouponId());
      ps.setString(3, usage.getUserId());
      ps.setString(4, usage.getOrderId());
      ps.setBigDecimal(5, usage.getDiscountApplied());
      ps.setTimestamp(6, toTimestamp(usage.getUsedAt()));
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(null, ps, con);
    }
  }

  public CouponUsage findByOrderId(String orderId) {
    Connection con = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("SELECT id, coupon_id, user_id, order_id, discount_applied, used_at FROM coupon_usage WHERE order_id = ?");
      ps.setString(1, orderId);
      rs = ps.executeQuery();
      if (rs.next()) {
        CouponUsage usage = new CouponUsage();
        usage.setId(rs.getString("id"));
        usage.setCouponId(rs.getString("coupon_id"));
        usage.setUserId(rs.getString("user_id"));
        usage.setOrderId(rs.getString("order_id"));
        usage.setDiscountApplied(rs.getBigDecimal("discount_applied"));
        usage.setUsedAt(rs.getTimestamp("used_at"));
        return usage;
      }
      return null;
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(rs, ps, con);
    }
  }

  public void deleteByOrderId(String orderId) {
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("DELETE FROM coupon_usage WHERE order_id = ?");
      ps.setString(1, orderId);
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
