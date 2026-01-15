package com.skishop.dao.user;

import com.skishop.common.dao.AbstractDao;
import com.skishop.common.dao.DaoException;
import com.skishop.domain.user.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class UserDaoImpl extends AbstractDao implements UserDao {
  public User findByEmail(String email) {
    Connection con = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("SELECT id, email, username, password_hash, salt, status, role, created_at, updated_at FROM users WHERE email = ?");
      ps.setString(1, email);
      rs = ps.executeQuery();
      if (rs.next()) {
        return mapUser(rs);
      }
      return null;
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(rs, ps, con);
    }
  }

  public User findById(String id) {
    Connection con = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("SELECT id, email, username, password_hash, salt, status, role, created_at, updated_at FROM users WHERE id = ?");
      ps.setString(1, id);
      rs = ps.executeQuery();
      if (rs.next()) {
        return mapUser(rs);
      }
      return null;
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(rs, ps, con);
    }
  }

  public void insert(User user) {
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("INSERT INTO users(id, email, username, password_hash, salt, status, role, created_at, updated_at) VALUES(?,?,?,?,?,?,?,?,?)");
      ps.setString(1, user.getId());
      ps.setString(2, user.getEmail());
      ps.setString(3, user.getUsername());
      ps.setString(4, user.getPasswordHash());
      ps.setString(5, user.getSalt());
      ps.setString(6, user.getStatus());
      ps.setString(7, user.getRole());
      ps.setTimestamp(8, toTimestamp(user.getCreatedAt()));
      ps.setTimestamp(9, toTimestamp(user.getUpdatedAt()));
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(null, ps, con);
    }
  }

  public void updateStatus(String userId, String status) {
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("UPDATE users SET status = ?, updated_at = ? WHERE id = ?");
      ps.setString(1, status);
      ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
      ps.setString(3, userId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(null, ps, con);
    }
  }

  private User mapUser(ResultSet rs) throws SQLException {
    User user = new User();
    user.setId(rs.getString("id"));
    user.setEmail(rs.getString("email"));
    user.setUsername(rs.getString("username"));
    user.setPasswordHash(rs.getString("password_hash"));
    user.setSalt(rs.getString("salt"));
    user.setStatus(rs.getString("status"));
    user.setRole(rs.getString("role"));
    user.setCreatedAt(rs.getTimestamp("created_at"));
    user.setUpdatedAt(rs.getTimestamp("updated_at"));
    return user;
  }

  private Timestamp toTimestamp(java.util.Date date) {
    if (date == null) {
      return null;
    }
    return new Timestamp(date.getTime());
  }
}
