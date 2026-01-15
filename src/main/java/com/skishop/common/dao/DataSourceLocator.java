package com.skishop.common.dao;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class DataSourceLocator {
  private static final DataSourceLocator instance = new DataSourceLocator();
  private DataSource dataSource;
  private boolean initialized;

  private DataSourceLocator() {
  }

  public static DataSourceLocator getInstance() {
    return instance;
  }

  public synchronized void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
    this.initialized = true;
  }

  public synchronized DataSource getDataSource() {
    if (dataSource != null) {
      return dataSource;
    }
    if (!initialized) {
      initialized = true;
      try {
        Context ctx = new InitialContext();
        dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/skishop");
      } catch (NamingException e) {
        throw new IllegalStateException("DataSource not configured", e);
      }
    }
    if (dataSource == null) {
      throw new IllegalStateException("DataSource not configured");
    }
    return dataSource;
  }
}
