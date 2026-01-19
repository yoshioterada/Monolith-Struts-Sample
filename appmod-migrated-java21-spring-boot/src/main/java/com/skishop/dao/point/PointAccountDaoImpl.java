package com.skishop.dao.point;

import com.skishop.common.dao.AbstractDao;
import com.skishop.common.dao.DaoException;
import com.skishop.domain.point.PointAccount;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.stereotype.Repository;

@Repository
public class PointAccountDaoImpl extends AbstractDao implements PointAccountDao {
  public PointAccount findByUserId(String userId) {
    var sql = "SELECT id, user_id, balance, lifetime_earned, lifetime_redeemed FROM point_accounts WHERE user_id = ?";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, userId);
      try (var rs = ps.executeQuery()) {
        if (rs.next()) {
          var account = new PointAccount();
          account.setId(rs.getString("id"));
          account.setUserId(rs.getString("user_id"));
          account.setBalance(rs.getInt("balance"));
          account.setLifetimeEarned(rs.getInt("lifetime_earned"));
          account.setLifetimeRedeemed(rs.getInt("lifetime_redeemed"));
          return account;
        }
      }
      return null;
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public void insert(PointAccount account) {
    var sql = "INSERT INTO point_accounts(id, user_id, balance, lifetime_earned, lifetime_redeemed) VALUES(?,?,?,?,?)";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, account.getId());
      ps.setString(2, account.getUserId());
      ps.setInt(3, account.getBalance());
      ps.setInt(4, account.getLifetimeEarned());
      ps.setInt(5, account.getLifetimeRedeemed());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public void increment(String userId, int amount) {
    var updateSql = amount >= 0
        ? "UPDATE point_accounts SET balance = balance + ?, lifetime_earned = lifetime_earned + ? WHERE user_id = ?"
        : "UPDATE point_accounts SET balance = balance + ?, lifetime_redeemed = lifetime_redeemed + ? WHERE user_id = ?";
    var absoluteAmount = Math.abs(amount);
    try (var con = getConnection(); var ps = con.prepareStatement(updateSql)) {
      ps.setInt(1, amount);
      ps.setInt(2, absoluteAmount);
      ps.setString(3, userId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }
}
