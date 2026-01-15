package com.skishop.domain.mail;

import java.util.Date;

public class EmailQueue {
  private String id;
  private String toAddr;
  private String subject;
  private String body;
  private String status;
  private int retryCount;
  private String lastError;
  private Date scheduledAt;
  private Date sentAt;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getToAddr() {
    return toAddr;
  }

  public void setToAddr(String toAddr) {
    this.toAddr = toAddr;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public int getRetryCount() {
    return retryCount;
  }

  public void setRetryCount(int retryCount) {
    this.retryCount = retryCount;
  }

  public String getLastError() {
    return lastError;
  }

  public void setLastError(String lastError) {
    this.lastError = lastError;
  }

  public Date getScheduledAt() {
    return scheduledAt;
  }

  public void setScheduledAt(Date scheduledAt) {
    this.scheduledAt = scheduledAt;
  }

  public Date getSentAt() {
    return sentAt;
  }

  public void setSentAt(Date sentAt) {
    this.sentAt = sentAt;
  }
}
