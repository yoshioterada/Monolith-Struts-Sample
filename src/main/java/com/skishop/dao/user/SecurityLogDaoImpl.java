package com.skishop.dao.user;

import com.skishop.common.dao.AbstractDao;
import com.skishop.common.dao.DaoException;
import com.skishop.domain.user.SecurityLog;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SecurityLogDaoImpl extends AbstractDao implements SecurityLogDao {
  public void insert(SecurityLog log) {
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = getConnection();
      ps = con.prepareStatement(
          "INSERT INTO security_logs(id, user_id, event_type, ip_address, user_agent, details_json) VALUES(?,?,?,?,?,?)");
      ps.setString(1, log.getId());
      ps.setString(2, log.getUserId());
      ps.setString(3, log.getEventType());
      ps.setString(4, log.getIpAddress());
      ps.setString(5, log.getUserAgent());
      ps.setString(6, log.getDetailsJson());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(null, ps, con);
    }
  }

  public int countByUserAndEvent(String userId, String eventType) {
    Connection con = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("SELECT COUNT(*) FROM security_logs WHERE user_id = ? AND event_type = ?");
      ps.setString(1, userId);
      ps.setString(2, eventType);
      rs = ps.executeQuery();
      if (rs.next()) {
        return rs.getInt(1);
      }
      return 0;
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(rs, ps, con);
    }
  }
}
