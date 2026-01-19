package com.skishop.dao.product;

import com.skishop.common.dao.AbstractDao;
import com.skishop.common.dao.DaoException;
import com.skishop.domain.product.Price;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.springframework.stereotype.Repository;

@Repository
public class PriceDaoImpl extends AbstractDao implements PriceDao {
  public Price findByProductId(String productId) {
    var sql = "SELECT id, product_id, regular_price, sale_price, currency_code, sale_start_date, sale_end_date FROM prices WHERE product_id = ?";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, productId);
      try (var rs = ps.executeQuery()) {
        if (rs.next()) {
          var price = new Price();
          price.setId(rs.getString("id"));
          price.setProductId(rs.getString("product_id"));
          price.setRegularPrice(rs.getBigDecimal("regular_price"));
          price.setSalePrice(rs.getBigDecimal("sale_price"));
          price.setCurrencyCode(rs.getString("currency_code"));
          price.setSaleStartDate(rs.getTimestamp("sale_start_date"));
          price.setSaleEndDate(rs.getTimestamp("sale_end_date"));
          return price;
        }
      }
      return null;
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  public void saveOrUpdate(Price price) {
    Price existing = findByProductId(price.getProductId());
    if (existing == null) {
      insert(price);
    } else {
      update(existing.getId(), price);
    }
  }

  private void insert(Price price) {
    var sql = "INSERT INTO prices(id, product_id, regular_price, sale_price, currency_code, sale_start_date, sale_end_date) VALUES(?,?,?,?,?,?,?)";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setString(1, price.getId());
      ps.setString(2, price.getProductId());
      ps.setBigDecimal(3, price.getRegularPrice());
      ps.setBigDecimal(4, price.getSalePrice());
      ps.setString(5, price.getCurrencyCode());
      ps.setTimestamp(6, toTimestamp(price.getSaleStartDate()));
      ps.setTimestamp(7, toTimestamp(price.getSaleEndDate()));
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException(e);
    }
  }

  private void update(String id, Price price) {
    var sql = "UPDATE prices SET regular_price = ?, sale_price = ?, currency_code = ?, sale_start_date = ?, sale_end_date = ? WHERE id = ?";
    try (var con = getConnection(); var ps = con.prepareStatement(sql)) {
      ps.setBigDecimal(1, price.getRegularPrice());
      ps.setBigDecimal(2, price.getSalePrice());
      ps.setString(3, price.getCurrencyCode());
      ps.setTimestamp(4, toTimestamp(price.getSaleStartDate()));
      ps.setTimestamp(5, toTimestamp(price.getSaleEndDate()));
      ps.setString(6, id);
      ps.executeUpdate();
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
