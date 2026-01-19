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
import org.springframework.stereotype.Repository;

@Repository
public class PointTransactionDaoImpl extends AbstractDao implements PointTransactionDao {
  public void insert(PointTransaction transaction) {
    var sql = "INSERT INTO point_transactions(id, user_id, type, amount, reference_id, description, expires_at, is_expired, created_at) VALUES(?,?,?,?,?,?,?,?,?)";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
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
    }
  }

  public List<PointTransaction> listByUserId(String userId) {
    var sql = "SELECT id, user_id, type, amount, reference_id, description, expires_at, is_expired, created_at FROM point_transactions WHERE user_id = ? ORDER BY created_at DESC";
    final var transactions = new ArrayList<PointTransaction>();
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, userId);
      try (var rs = ps.executeQuery()) {
        while (rs.next()) {
          var transaction = new PointTransaction();
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
      }
      return transactions;
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
