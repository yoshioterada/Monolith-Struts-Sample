package com.skishop.common.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class AbstractDao {
  protected Connection getConnection() throws SQLException {
    return DataSourceLocator.getInstance().getDataSource().getConnection();
  }

  protected void closeQuietly(ResultSet rs, Statement st, Connection con) {
    try {
      if (rs != null) {
        rs.close();
      }
    } catch (Exception e) {
    }
    try {
      if (st != null) {
        st.close();
      }
    } catch (Exception e) {
    }
    try {
      if (con != null) {
        con.close();
      }
    } catch (Exception e) {
    }
  }
}
