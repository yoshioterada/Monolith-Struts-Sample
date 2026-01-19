package com.skishop.dao.shipping;

import com.skishop.common.dao.AbstractDao;
import com.skishop.common.dao.DaoException;
import com.skishop.domain.shipping.ShippingMethod;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ShippingMethodDaoImpl extends AbstractDao implements ShippingMethodDao {
  public List<ShippingMethod> listActive() {
    var sql = "SELECT id, code, name, fee, is_active, sort_order FROM shipping_methods WHERE is_active = TRUE ORDER BY sort_order";
    final var methods = new ArrayList<ShippingMethod>();
    try (var con = getConnection(); var ps = con.prepareStatement(sql); var rs = ps.executeQuery()) {
      while (rs.next()) {
        methods.add(mapMethod(rs));
      }
      return methods;
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public List<ShippingMethod> listAll() {
    var sql = "SELECT id, code, name, fee, is_active, sort_order FROM shipping_methods ORDER BY sort_order";
    final var methods = new ArrayList<ShippingMethod>();
    try (var con = getConnection(); var ps = con.prepareStatement(sql); var rs = ps.executeQuery()) {
      while (rs.next()) {
        methods.add(mapMethod(rs));
      }
      return methods;
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public ShippingMethod findByCode(String code) {
    var sql = "SELECT id, code, name, fee, is_active, sort_order FROM shipping_methods WHERE code = ?";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, code);
      try (var rs = ps.executeQuery()) {
        if (rs.next()) {
          return mapMethod(rs);
        }
      }
      return null;
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public void insert(ShippingMethod method) {
    var sql = "INSERT INTO shipping_methods(id, code, name, fee, is_active, sort_order) VALUES(?,?,?,?,?,?)";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, method.getId());
      ps.setString(2, method.getCode());
      ps.setString(3, method.getName());
      ps.setBigDecimal(4, method.getFee());
      ps.setBoolean(5, method.isActive());
      ps.setInt(6, method.getSortOrder());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public void update(ShippingMethod method) {
    var sql = "UPDATE shipping_methods SET name = ?, fee = ?, is_active = ?, sort_order = ? WHERE code = ?";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, method.getName());
      ps.setBigDecimal(2, method.getFee());
      ps.setBoolean(3, method.isActive());
      ps.setInt(4, method.getSortOrder());
      ps.setString(5, method.getCode());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  private ShippingMethod mapMethod(ResultSet rs) throws SQLException {
    ShippingMethod method = new ShippingMethod();
    method.setId(rs.getString("id"));
    method.setCode(rs.getString("code"));
    method.setName(rs.getString("name"));
    method.setFee(rs.getBigDecimal("fee"));
    method.setActive(rs.getBoolean("is_active"));
    method.setSortOrder(rs.getInt("sort_order"));
    return method;
  }
}
