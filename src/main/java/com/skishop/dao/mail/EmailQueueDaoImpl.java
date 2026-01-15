package com.skishop.dao.mail;

import com.skishop.common.dao.AbstractDao;
import com.skishop.common.dao.DaoException;
import com.skishop.domain.mail.EmailQueue;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class EmailQueueDaoImpl extends AbstractDao implements EmailQueueDao {
  public void enqueue(EmailQueue mail) {
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("INSERT INTO email_queue(id, to_addr, subject, body, status, retry_count, last_error, scheduled_at, sent_at) VALUES(?,?,?,?,?,?,?,?,?)");
      ps.setString(1, mail.getId());
      ps.setString(2, mail.getToAddr());
      ps.setString(3, mail.getSubject());
      ps.setString(4, mail.getBody());
      ps.setString(5, mail.getStatus());
      ps.setInt(6, mail.getRetryCount());
      ps.setString(7, mail.getLastError());
      ps.setTimestamp(8, toTimestamp(mail.getScheduledAt()));
      ps.setTimestamp(9, toTimestamp(mail.getSentAt()));
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(null, ps, con);
    }
  }

  public void updateStatus(String id, String status, int retryCount, String lastError) {
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = getConnection();
      ps = con.prepareStatement("UPDATE email_queue SET status = ?, retry_count = ?, last_error = ? WHERE id = ?");
      ps.setString(1, status);
      ps.setInt(2, retryCount);
      ps.setString(3, lastError);
      ps.setString(4, id);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(null, ps, con);
    }
  }

  public List findByStatus(String status) {
    Connection con = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    List mails = new ArrayList();
    try {
      con = getConnection();
      ps = con.prepareStatement("SELECT id, to_addr, subject, body, status, retry_count, last_error, scheduled_at, sent_at FROM email_queue WHERE status = ? ORDER BY scheduled_at");
      ps.setString(1, status);
      rs = ps.executeQuery();
      while (rs.next()) {
        EmailQueue mail = new EmailQueue();
        mail.setId(rs.getString("id"));
        mail.setToAddr(rs.getString("to_addr"));
        mail.setSubject(rs.getString("subject"));
        mail.setBody(rs.getString("body"));
        mail.setStatus(rs.getString("status"));
        mail.setRetryCount(rs.getInt("retry_count"));
        mail.setLastError(rs.getString("last_error"));
        mail.setScheduledAt(rs.getTimestamp("scheduled_at"));
        mail.setSentAt(rs.getTimestamp("sent_at"));
        mails.add(mail);
      }
      return mails;
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
