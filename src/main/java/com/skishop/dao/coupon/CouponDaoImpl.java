package com.skishop.dao.coupon;

import com.skishop.common.dao.AbstractDao;
import com.skishop.common.dao.DaoException;
import com.skishop.domain.coupon.Coupon;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CouponDaoImpl extends AbstractDao implements CouponDao {
  public Coupon findByCode(String code) {
    Connection con = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("SELECT id, campaign_id, code, coupon_type, discount_value, discount_type, minimum_amount, maximum_discount, usage_limit, used_count, is_active, expires_at FROM coupons WHERE code = ?");
      ps.setString(1, code);
      rs = ps.executeQuery();
      if (rs.next()) {
        Coupon coupon = new Coupon();
        coupon.setId(rs.getString("id"));
        coupon.setCampaignId(rs.getString("campaign_id"));
        coupon.setCode(rs.getString("code"));
        coupon.setCouponType(rs.getString("coupon_type"));
        coupon.setDiscountValue(rs.getBigDecimal("discount_value"));
        coupon.setDiscountType(rs.getString("discount_type"));
        coupon.setMinimumAmount(rs.getBigDecimal("minimum_amount"));
        coupon.setMaximumDiscount(rs.getBigDecimal("maximum_discount"));
        coupon.setUsageLimit(rs.getInt("usage_limit"));
        coupon.setUsedCount(rs.getInt("used_count"));
        coupon.setActive(rs.getBoolean("is_active"));
        coupon.setExpiresAt(rs.getTimestamp("expires_at"));
        return coupon;
      }
      return null;
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(rs, ps, con);
    }
  }

  public void incrementUsedCount(String couponId) {
    updateUsageCount(couponId, 1);
  }

  public void decrementUsedCount(String couponId) {
    updateUsageCount(couponId, -1);
  }

  private void updateUsageCount(String couponId, int delta) {
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("UPDATE coupons SET used_count = used_count + ? WHERE id = ?");
      ps.setInt(1, delta);
      ps.setString(2, couponId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(null, ps, con);
    }
  }
}
