package com.skishop.web.action;

import com.skishop.common.dao.DataSourceLocator;
import com.skishop.domain.user.User;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.logging.Logger;
import org.h2.jdbcx.JdbcDataSource;
import org.apache.struts.util.TokenProcessor;
import servletunit.HttpServletRequestSimulator;
import servletunit.struts.MockStrutsTestCase;

public abstract class StrutsActionTestBase extends MockStrutsTestCase {
  private static final Logger LOGGER = Logger.getLogger(StrutsActionTestBase.class.getName());
  private static final String TOKEN_REQUEST_KEY = "org.apache.struts.taglib.html.TOKEN";
  private static final String TOKEN_SESSION_KEY = "org.apache.struts.action.TOKEN";
  private static JdbcDataSource dataSource;

  protected void setUp() throws Exception {
    super.setUp();
    setContextDirectory(new File("src/main/webapp"));
    setConfigFile("/WEB-INF/struts-config.xml");
    initDataSource();
    resetDatabase();
  }

  private static synchronized void initDataSource() {
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
      } catch (java.sql.SQLException e) {
        LOGGER.fine("Test cleanup warning: " + e.getMessage());
      }
      try {
        if (reader != null) {
          reader.close();
        }
      } catch (java.io.IOException e) {
        LOGGER.fine("Test cleanup warning: " + e.getMessage());
      }
      try {
        if (stream != null) {
          stream.close();
        }
      } catch (java.io.IOException e) {
        LOGGER.fine("Test cleanup warning: " + e.getMessage());
      }
    }
  }

  protected void setPostRequest() {
    HttpServletRequestSimulator request = (HttpServletRequestSimulator) getRequest();
    request.setMethod(HttpServletRequestSimulator.POST);
    TokenProcessor.getInstance().saveToken(getRequest());
    String token = (String) getRequest().getSession().getAttribute(TOKEN_SESSION_KEY);
    if (token != null) {
      addRequestParameter(TOKEN_REQUEST_KEY, token);
    }
  }

  protected void setGetRequest() {
    HttpServletRequestSimulator request = (HttpServletRequestSimulator) getRequest();
    request.setMethod(HttpServletRequestSimulator.GET);
  }

  protected void setLoginUser(String userId, String role) {
    User user = new User();
    user.setId(userId);
    user.setRole(role);
    getSession().setAttribute("loginUser", user);
  }
}
