package com.skishop.service.point;

import com.skishop.common.dao.DataSourceLocator;
import com.skishop.dao.point.PointAccountDao;
import com.skishop.dao.point.PointTransactionDao;
import com.skishop.domain.point.PointAccount;
import com.skishop.domain.point.PointTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

@Service
public class PointService {
  private final PointAccountDao pointAccountDao;
  private final PointTransactionDao pointTransactionDao;

  @Autowired
  public PointService(PointAccountDao pointAccountDao, PointTransactionDao pointTransactionDao) {
    this.pointAccountDao = pointAccountDao;
    this.pointTransactionDao = pointTransactionDao;
  }

  public int awardPoints(String userId, String referenceId, BigDecimal totalAmount) {
    if (userId == null || totalAmount == null) {
      return 0;
    }
    int points = calculateAwardPoints(totalAmount);
    if (points <= 0) {
      return 0;
    }
    ensureAccount(userId);
    pointAccountDao.increment(userId, points);

    PointTransaction transaction = new PointTransaction();
    transaction.setId(UUID.randomUUID().toString());
    transaction.setUserId(userId);
    transaction.setType("EARN");
    transaction.setAmount(points);
    transaction.setReferenceId(referenceId);
    transaction.setDescription("Order points");
    transaction.setExpiresAt(addDays(new Date(), 365));
    transaction.setExpired(false);
    transaction.setCreatedAt(new Date());
    pointTransactionDao.insert(transaction);
    return points;
  }

  public void redeemPoints(String userId, int points, String referenceId) {
    if (points <= 0) {
      return;
    }
    expirePoints(userId);
    PointAccount account = ensureAccount(userId);
    if (account.getBalance() < points) {
      throw new IllegalArgumentException("Insufficient points");
    }
    pointAccountDao.increment(userId, -points);
    PointTransaction transaction = buildTransaction(userId, "REDEEM", -points, referenceId, "Redeem points");
    pointTransactionDao.insert(transaction);
  }

  public void refundPoints(String userId, int points, String referenceId) {
    if (points <= 0) {
      return;
    }
    ensureAccount(userId);
    pointAccountDao.increment(userId, points);
    PointTransaction transaction = buildTransaction(userId, "REFUND", points, referenceId, "Refund points");
    pointTransactionDao.insert(transaction);
  }

  public void revokePoints(String userId, int points, String referenceId) {
    if (points <= 0) {
      return;
    }
    ensureAccount(userId);
    pointAccountDao.increment(userId, -points);
    PointTransaction transaction = buildTransaction(userId, "REVOKE", -points, referenceId, "Revoke points");
    pointTransactionDao.insert(transaction);
  }

  public int calculateAwardPoints(BigDecimal totalAmount) {
    if (totalAmount == null) {
      return 0;
    }
    BigDecimal points = totalAmount.multiply(new BigDecimal("0.01"));
    return points.setScale(0, RoundingMode.DOWN).intValue();
  }

  public PointAccount getAccount(String userId) {
    expirePoints(userId);
    return ensureAccount(userId);
  }

  private PointAccount ensureAccount(String userId) {
    PointAccount account = pointAccountDao.findByUserId(userId);
    if (account == null) {
      account = new PointAccount();
      account.setId(UUID.randomUUID().toString());
      account.setUserId(userId);
      account.setBalance(0);
      account.setLifetimeEarned(0);
      account.setLifetimeRedeemed(0);
      pointAccountDao.insert(account);
    }
    return account;
  }

  private PointTransaction buildTransaction(String userId, String type, int amount, String referenceId, String description) {
    PointTransaction transaction = new PointTransaction();
    transaction.setId(UUID.randomUUID().toString());
    transaction.setUserId(userId);
    transaction.setType(type);
    transaction.setAmount(amount);
    transaction.setReferenceId(referenceId);
    transaction.setDescription(description);
    transaction.setExpiresAt(null);
    transaction.setExpired(false);
    transaction.setCreatedAt(new Date());
    return transaction;
  }

  private void expirePoints(String userId) {
    if (userId == null) {
      return;
    }
    Connection con = null;
    try {
      con = DataSourceLocator.getInstance().getDataSource().getConnection();
      con.setAutoCommit(false);
      var expirationClause = "user_id = ? AND is_expired = FALSE AND expires_at IS NOT NULL AND expires_at < ?";
      var now = new java.sql.Timestamp(System.currentTimeMillis());

      try (var lockPs = con.prepareStatement("SELECT id FROM point_accounts WHERE user_id = ? FOR UPDATE")) {
        lockPs.setString(1, userId);
        try (var lockRs = lockPs.executeQuery()) {
          // no-op; just lock
        }
      }

      int expiredAmount = 0;
      try (var sumPs = con.prepareStatement("SELECT COALESCE(SUM(amount), 0) FROM point_transactions WHERE " + expirationClause)) {
        sumPs.setString(1, userId);
        sumPs.setTimestamp(2, now);
        try (var rs = sumPs.executeQuery()) {
          if (rs.next()) {
            expiredAmount = rs.getInt(1);
          }
        }
      }

      if (expiredAmount > 0) {
        try (var updatePs = con.prepareStatement("UPDATE point_transactions SET is_expired = TRUE WHERE " + expirationClause)) {
          updatePs.setString(1, userId);
          updatePs.setTimestamp(2, now);
          updatePs.executeUpdate();
        }
        try (var accountPs = con.prepareStatement("UPDATE point_accounts SET balance = balance - ? WHERE user_id = ?")) {
          accountPs.setInt(1, expiredAmount);
          accountPs.setString(2, userId);
          accountPs.executeUpdate();
        }
      }
      con.commit();
    } catch (SQLException e) {
      rollbackQuietly(con);
      throw new IllegalStateException("Failed to expire points", e);
    } finally {
      closeQuietly(con);
    }
  }

  private Date addDays(Date base, int days) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(base);
    cal.add(Calendar.DAY_OF_YEAR, days);
    return cal.getTime();
  }

  private void rollbackQuietly(Connection con) {
    if (con != null) {
      try {
        con.rollback();
      } catch (SQLException e) {
        // ignore rollback errors
      }
    }
  }

  private void closeQuietly(ResultSet rs) {
    if (rs != null) {
      try {
        rs.close();
      } catch (SQLException e) {
        // ignore cleanup errors
      }
    }
  }

  private void closeQuietly(PreparedStatement ps) {
    if (ps != null) {
      try {
        ps.close();
      } catch (SQLException e) {
        // ignore cleanup errors
      }
    }
  }

  private void closeQuietly(Connection con) {
    if (con != null) {
      try {
        con.close();
      } catch (SQLException e) {
        // ignore cleanup errors
      }
    }
  }
}
