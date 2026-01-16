package com.skishop.domain.user;

public class SecurityLog {
  private String id;
  private String userId;
  private String eventType;
  private String ipAddress;
  private String userAgent;
  private String detailsJson;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  public String getDetailsJson() {
    return detailsJson;
  }

  public void setDetailsJson(String detailsJson) {
    this.detailsJson = detailsJson;
  }
}
