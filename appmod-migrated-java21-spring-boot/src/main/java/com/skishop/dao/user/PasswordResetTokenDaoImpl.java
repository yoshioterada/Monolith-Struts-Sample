package com.skishop.dao.user;

import com.skishop.common.dao.AbstractDao;
import com.skishop.common.dao.DaoException;
import com.skishop.domain.user.PasswordResetToken;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.springframework.stereotype.Repository;

@Repository
public class PasswordResetTokenDaoImpl extends AbstractDao implements PasswordResetTokenDao {
  public void insert(PasswordResetToken token) {
    var sql = "INSERT INTO password_reset_tokens(id, user_id, token, expires_at, used_at) VALUES(?,?,?,?,?)";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, token.getId());
      ps.setString(2, token.getUserId());
      ps.setString(3, token.getToken());
      ps.setTimestamp(4, toTimestamp(token.getExpiresAt()));
      ps.setTimestamp(5, toTimestamp(token.getUsedAt()));
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public PasswordResetToken findByToken(String tokenValue) {
    var sql = "SELECT id, user_id, token, expires_at, used_at FROM password_reset_tokens WHERE token = ?";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, tokenValue);
      try (var rs = ps.executeQuery()) {
        if (rs.next()) {
          var token = new PasswordResetToken();
          token.setId(rs.getString("id"));
          token.setUserId(rs.getString("user_id"));
          token.setToken(rs.getString("token"));
          token.setExpiresAt(rs.getTimestamp("expires_at"));
          token.setUsedAt(rs.getTimestamp("used_at"));
          return token;
        }
      }
      return null;
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public void markUsed(String tokenId) {
    var sql = "UPDATE password_reset_tokens SET used_at = ? WHERE id = ?";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
      ps.setString(2, tokenId);
      ps.executeUpdate();
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
