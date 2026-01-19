package com.skishop.dao.user;

import com.skishop.common.dao.AbstractDao;
import com.skishop.common.dao.DaoException;
import com.skishop.domain.user.SecurityLog;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.stereotype.Repository;

@Repository
public class SecurityLogDaoImpl extends AbstractDao implements SecurityLogDao {
  public void insert(SecurityLog log) {
    var sql = "INSERT INTO security_logs(id, user_id, event_type, ip_address, user_agent, details_json) VALUES(?,?,?,?,?,?)";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, log.getId());
      ps.setString(2, log.getUserId());
      ps.setString(3, log.getEventType());
      ps.setString(4, log.getIpAddress());
      ps.setString(5, log.getUserAgent());
      ps.setString(6, log.getDetailsJson());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public int countByUserAndEvent(String userId, String eventType) {
    var sql = "SELECT COUNT(*) FROM security_logs WHERE user_id = ? AND event_type = ?";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, userId);
      ps.setString(2, eventType);
      try (var rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1);
        }
      }
      return 0;
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }
}
