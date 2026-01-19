package com.skishop.common.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {
  private static final String CONFIG_FILE = "app.properties";
  private static final AppConfig INSTANCE = new AppConfig();
  private final Properties properties = new Properties();

  private AppConfig() {
    load();
  }

  public static AppConfig getInstance() {
    return INSTANCE;
  }

  public String getString(String key) {
    var value = properties.getProperty(key);
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  public int getInt(String key, int defaultValue) {
    String value = getString(key);
    if (value == null) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private void load() {
    try (var input = AppConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
      if (input == null) {
        throw new IllegalStateException("app.properties not found");
      }
      properties.load(input);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load app.properties", e);
    }
    resolvePlaceholders();
  }

  private void resolvePlaceholders() {
    var resolved = new Properties();
    for (var key : properties.stringPropertyNames()) {
      resolved.setProperty(key, resolveValue(properties.getProperty(key)));
    }
    properties.clear();
    properties.putAll(resolved);
  }

  private String resolveValue(String value) {
    if (value == null) {
      return null;
    }
    var resolved = new StringBuilder();
    var index = 0;
    var replaced = false;
    while (true) {
      int start = value.indexOf("${", index);
      if (start < 0) {
        break;
      }
      int end = value.indexOf("}", start + 2);
      if (end < 0) {
        break;
      }
      resolved.append(value.substring(index, start));
      var token = value.substring(start + 2, end);
      var replacement = System.getProperty(token);
      if (replacement == null) {
        replacement = System.getenv(token);
      }
      if (replacement == null) {
        replacement = "";
      }
      resolved.append(replacement);
      index = end + 1;
      replaced = true;
    }
    if (!replaced) {
      return value;
    }
    resolved.append(value.substring(index));
    return resolved.toString();
  }
}
