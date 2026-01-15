package com.skishop.dao.point;

import com.skishop.common.dao.AbstractDao;
import com.skishop.common.dao.DaoException;
import com.skishop.domain.point.PointTransaction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PointTransactionDaoImpl extends AbstractDao implements PointTransactionDao {
  public void insert(PointTransaction transaction) {
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("INSERT INTO point_transactions(id, user_id, type, amount, reference_id, description, expires_at, is_expired, created_at) VALUES(?,?,?,?,?,?,?,?,?)");
      ps.setString(1, transaction.getId());
      ps.setString(2, transaction.getUserId());
      ps.setString(3, transaction.getType());
      ps.setInt(4, transaction.getAmount());
      ps.setString(5, transaction.getReferenceId());
      ps.setString(6, transaction.getDescription());
      ps.setTimestamp(7, toTimestamp(transaction.getExpiresAt()));
      ps.setBoolean(8, transaction.isExpired());
      ps.setTimestamp(9, toTimestamp(transaction.getCreatedAt()));
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(null, ps, con);
    }
  }

  public List listByUserId(String userId) {
    Connection con = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    List transactions = new ArrayList();
    try {
      con = getConnection();
      ps = con.prepareStatement("SELECT id, user_id, type, amount, reference_id, description, expires_at, is_expired, created_at FROM point_transactions WHERE user_id = ? ORDER BY created_at DESC");
      ps.setString(1, userId);
      rs = ps.executeQuery();
      while (rs.next()) {
        PointTransaction transaction = new PointTransaction();
        transaction.setId(rs.getString("id"));
        transaction.setUserId(rs.getString("user_id"));
        transaction.setType(rs.getString("type"));
        transaction.setAmount(rs.getInt("amount"));
        transaction.setReferenceId(rs.getString("reference_id"));
        transaction.setDescription(rs.getString("description"));
        transaction.setExpiresAt(rs.getTimestamp("expires_at"));
        transaction.setExpired(rs.getBoolean("is_expired"));
        transaction.setCreatedAt(rs.getTimestamp("created_at"));
        transactions.add(transaction);
      }
      return transactions;
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(rs, ps, con);
    }
  }

  private Timestamp toTimestamp(java.util.Date date) {
    if (date == null) {
      return null;
    }
    return new Timestamp(date.getTime());
  }
}
