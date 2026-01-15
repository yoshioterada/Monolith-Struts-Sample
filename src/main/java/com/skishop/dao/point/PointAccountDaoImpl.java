package com.skishop.dao.point;

import com.skishop.common.dao.AbstractDao;
import com.skishop.common.dao.DaoException;
import com.skishop.domain.point.PointAccount;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PointAccountDaoImpl extends AbstractDao implements PointAccountDao {
  public PointAccount findByUserId(String userId) {
    Connection con = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("SELECT id, user_id, balance, lifetime_earned, lifetime_redeemed FROM point_accounts WHERE user_id = ?");
      ps.setString(1, userId);
      rs = ps.executeQuery();
      if (rs.next()) {
        PointAccount account = new PointAccount();
        account.setId(rs.getString("id"));
        account.setUserId(rs.getString("user_id"));
        account.setBalance(rs.getInt("balance"));
        account.setLifetimeEarned(rs.getInt("lifetime_earned"));
        account.setLifetimeRedeemed(rs.getInt("lifetime_redeemed"));
        return account;
      }
      return null;
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(rs, ps, con);
    }
  }

  public void insert(PointAccount account) {
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("INSERT INTO point_accounts(id, user_id, balance, lifetime_earned, lifetime_redeemed) VALUES(?,?,?,?,?)");
      ps.setString(1, account.getId());
      ps.setString(2, account.getUserId());
      ps.setInt(3, account.getBalance());
      ps.setInt(4, account.getLifetimeEarned());
      ps.setInt(5, account.getLifetimeRedeemed());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(null, ps, con);
    }
  }

  public void increment(String userId, int amount) {
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = getConnection();
      String sql;
      int metricAmount;
      if (amount >= 0) {
        sql = "UPDATE point_accounts SET balance = balance + ?, lifetime_earned = lifetime_earned + ? WHERE user_id = ?";
        metricAmount = amount;
      } else {
        sql = "UPDATE point_accounts SET balance = balance + ?, lifetime_redeemed = lifetime_redeemed + ? WHERE user_id = ?";
        metricAmount = Math.abs(amount);
      }
      ps = con.prepareStatement(sql);
      ps.setInt(1, amount);
      ps.setInt(2, metricAmount);
      ps.setString(3, userId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(null, ps, con);
    }
  }
}
