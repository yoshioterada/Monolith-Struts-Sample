package com.skishop.dao.coupon;

import com.skishop.common.dao.AbstractDao;
import com.skishop.common.dao.DaoException;
import com.skishop.domain.coupon.Coupon;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class CouponDaoImpl extends AbstractDao implements CouponDao {
  public Coupon findByCode(String code) {
    var sql = "SELECT id, campaign_id, code, coupon_type, discount_value, discount_type, minimum_amount, maximum_discount, usage_limit, used_count, is_active, expires_at FROM coupons WHERE code = ?";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, code);
      try (var rs = ps.executeQuery()) {
        if (rs.next()) {
          return mapCoupon(rs);
        }
      }
      return null;
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public List<Coupon> listActive() {
    var sql = "SELECT id, campaign_id, code, coupon_type, discount_value, discount_type, minimum_amount, maximum_discount, usage_limit, used_count, is_active, expires_at FROM coupons WHERE is_active = ? AND (expires_at IS NULL OR expires_at > ?) ORDER BY code";
    final var coupons = new ArrayList<Coupon>();
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setBoolean(1, true);
      ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
      try (var rs = ps.executeQuery()) {
        while (rs.next()) {
          coupons.add(mapCoupon(rs));
        }
      }
      return coupons;
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public void incrementUsedCount(String couponId) {
    updateUsageCount(couponId, 1);
  }

  public List<Coupon> listAll() {
    var sql = "SELECT id, campaign_id, code, coupon_type, discount_value, discount_type, minimum_amount, maximum_discount, usage_limit, used_count, is_active, expires_at FROM coupons ORDER BY code";
    final var coupons = new ArrayList<Coupon>();
    try (var con = getConnection(); var ps = con.prepareStatement(sql); var rs = ps.executeQuery()) {
      while (rs.next()) {
        coupons.add(mapCoupon(rs));
      }
      return coupons;
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public void saveOrUpdate(Coupon coupon) {
    Coupon existing = findByCode(coupon.getCode());
    if (existing == null) {
      insert(coupon);
    } else {
      coupon.setId(existing.getId());
      coupon.setUsedCount(existing.getUsedCount());
      update(coupon);
    }
  }

  public void decrementUsedCount(String couponId) {
    updateUsageCount(couponId, -1);
  }

  private void updateUsageCount(String couponId, int delta) {
    var sql = "UPDATE coupons SET used_count = used_count + ? WHERE id = ?";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setInt(1, delta);
      ps.setString(2, couponId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  private void insert(Coupon coupon) {
    var sql = "INSERT INTO coupons(id, campaign_id, code, coupon_type, discount_value, discount_type, minimum_amount, maximum_discount, usage_limit, used_count, is_active, expires_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, coupon.getId());
      ps.setString(2, coupon.getCampaignId());
      ps.setString(3, coupon.getCode());
      ps.setString(4, coupon.getCouponType());
      ps.setBigDecimal(5, coupon.getDiscountValue());
      ps.setString(6, coupon.getDiscountType());
      ps.setBigDecimal(7, coupon.getMinimumAmount());
      ps.setBigDecimal(8, coupon.getMaximumDiscount());
      ps.setInt(9, coupon.getUsageLimit());
      ps.setInt(10, coupon.getUsedCount());
      ps.setBoolean(11, coupon.isActive());
      ps.setTimestamp(12, coupon.getExpiresAt() != null ? new Timestamp(coupon.getExpiresAt().getTime()) : null);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  private void update(Coupon coupon) {
    var sql = "UPDATE coupons SET campaign_id = ?, coupon_type = ?, discount_value = ?, discount_type = ?, minimum_amount = ?, maximum_discount = ?, usage_limit = ?, used_count = ?, is_active = ?, expires_at = ? WHERE code = ?";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, coupon.getCampaignId());
      ps.setString(2, coupon.getCouponType());
      ps.setBigDecimal(3, coupon.getDiscountValue());
      ps.setString(4, coupon.getDiscountType());
      ps.setBigDecimal(5, coupon.getMinimumAmount());
      ps.setBigDecimal(6, coupon.getMaximumDiscount());
      ps.setInt(7, coupon.getUsageLimit());
      ps.setInt(8, coupon.getUsedCount());
      ps.setBoolean(9, coupon.isActive());
      ps.setTimestamp(10, coupon.getExpiresAt() != null ? new Timestamp(coupon.getExpiresAt().getTime()) : null);
      ps.setString(11, coupon.getCode());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  private Coupon mapCoupon(ResultSet rs) throws SQLException {
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
}
