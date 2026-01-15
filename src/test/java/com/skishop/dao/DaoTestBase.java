package com.skishop.dao;

import com.skishop.common.dao.DataSourceLocator;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.BeforeClass;

public abstract class DaoTestBase {
  private static JdbcDataSource dataSource;

  @BeforeClass
  public static void initDataSource() {
    if (dataSource == null) {
      dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:skishop;DB_CLOSE_DELAY=-1");
      dataSource.setUser("sa");
      dataSource.setPassword("");
      DataSourceLocator.getInstance().setDataSource(dataSource);
    }
  }

  protected void resetDatabase() throws Exception {
    Connection con = null;
    Statement st = null;
    try {
      con = dataSource.getConnection();
      st = con.createStatement();
      st.execute("DROP ALL OBJECTS");
      runScript(con, "/db/schema.sql");
      runScript(con, "/db/data.sql");
    } finally {
      if (st != null) {
        st.close();
      }
      if (con != null) {
        con.close();
      }
    }
  }

  private void runScript(Connection con, String path) throws Exception {
    InputStream stream = getClass().getResourceAsStream(path);
    if (stream == null) {
      throw new IllegalStateException("SQL resource not found: " + path);
    }
    BufferedReader reader = null;
    Statement statement = null;
    try {
      reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
      StringBuilder buffer = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        buffer.append(line);
        buffer.append('\n');
      }
      String[] statements = buffer.toString().split(";");
      statement = con.createStatement();
      for (int i = 0; i < statements.length; i++) {
        String sql = statements[i].trim();
        if (sql.length() > 0) {
          statement.execute(sql);
        }
      }
    } finally {
      try {
        if (statement != null) {
          statement.close();
        }
      } catch (Exception e) {
        // ignore cleanup errors in tests
      }
      try {
        if (reader != null) {
          reader.close();
        }
      } catch (Exception e) {
        // ignore cleanup errors in tests
      }
      try {
        if (stream != null) {
          stream.close();
        }
      } catch (Exception e) {
        // ignore cleanup errors in tests
      }
    }
  }
}
