package com.skishop.dao.user;

import com.skishop.common.dao.AbstractDao;
import com.skishop.common.dao.DaoException;
import com.skishop.domain.user.PasswordResetToken;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class PasswordResetTokenDaoImpl extends AbstractDao implements PasswordResetTokenDao {
  public void insert(PasswordResetToken token) {
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("INSERT INTO password_reset_tokens(id, user_id, token, expires_at, used_at) VALUES(?,?,?,?,?)");
      ps.setString(1, token.getId());
      ps.setString(2, token.getUserId());
      ps.setString(3, token.getToken());
      ps.setTimestamp(4, toTimestamp(token.getExpiresAt()));
      ps.setTimestamp(5, toTimestamp(token.getUsedAt()));
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(null, ps, con);
    }
  }

  public PasswordResetToken findByToken(String tokenValue) {
    Connection con = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("SELECT id, user_id, token, expires_at, used_at FROM password_reset_tokens WHERE token = ?");
      ps.setString(1, tokenValue);
      rs = ps.executeQuery();
      if (rs.next()) {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(rs.getString("id"));
        token.setUserId(rs.getString("user_id"));
        token.setToken(rs.getString("token"));
        token.setExpiresAt(rs.getTimestamp("expires_at"));
        token.setUsedAt(rs.getTimestamp("used_at"));
        return token;
      }
      return null;
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(rs, ps, con);
    }
  }

  public void markUsed(String tokenId) {
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("UPDATE password_reset_tokens SET used_at = ? WHERE id = ?");
      ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
      ps.setString(2, tokenId);
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
