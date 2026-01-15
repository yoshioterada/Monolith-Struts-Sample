package com.skishop.dao.address;

import com.skishop.common.dao.AbstractDao;
import com.skishop.common.dao.DaoException;
import com.skishop.domain.address.Address;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class UserAddressDaoImpl extends AbstractDao implements UserAddressDao {
  public List listByUserId(String userId) {
    Connection con = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    List addresses = new ArrayList();
    try {
      con = getConnection();
      ps = con.prepareStatement("SELECT id, user_id, label, recipient_name, postal_code, prefecture, address1, address2, phone, is_default, created_at, updated_at FROM user_addresses WHERE user_id = ? ORDER BY is_default DESC, created_at");
      ps.setString(1, userId);
      rs = ps.executeQuery();
      while (rs.next()) {
        Address address = new Address();
        address.setId(rs.getString("id"));
        address.setUserId(rs.getString("user_id"));
        address.setLabel(rs.getString("label"));
        address.setRecipientName(rs.getString("recipient_name"));
        address.setPostalCode(rs.getString("postal_code"));
        address.setPrefecture(rs.getString("prefecture"));
        address.setAddress1(rs.getString("address1"));
        address.setAddress2(rs.getString("address2"));
        address.setPhone(rs.getString("phone"));
        address.setDefault(rs.getBoolean("is_default"));
        address.setCreatedAt(rs.getTimestamp("created_at"));
        address.setUpdatedAt(rs.getTimestamp("updated_at"));
        addresses.add(address);
      }
      return addresses;
    } catch (SQLException e) {
      throw new DaoException(e);
    } finally {
      closeQuietly(rs, ps, con);
    }
  }

  public void save(Address address) {
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = getConnection();
      if (address.isDefault()) {
        ps = con.prepareStatement("UPDATE user_addresses SET is_default = FALSE WHERE user_id = ?");
        ps.setString(1, address.getUserId());
        ps.executeUpdate();
        ps.close();
      }
      ps = con.prepareStatement("INSERT INTO user_addresses(id, user_id, label, recipient_name, postal_code, prefecture, address1, address2, phone, is_default, created_at, updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)");
      ps.setString(1, address.getId());
      ps.setString(2, address.getUserId());
      ps.setString(3, address.getLabel());
      ps.setString(4, address.getRecipientName());
      ps.setString(5, address.getPostalCode());
      ps.setString(6, address.getPrefecture());
      ps.setString(7, address.getAddress1());
      ps.setString(8, address.getAddress2());
      ps.setString(9, address.getPhone());
      ps.setBoolean(10, address.isDefault());
      ps.setTimestamp(11, toTimestamp(address.getCreatedAt()));
      ps.setTimestamp(12, toTimestamp(address.getUpdatedAt()));
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
