package com.skishop.domain.coupon;

import java.util.Date;

public class Campaign {
  private String id;
  private String name;
  private String description;
  private String type;
  private Date startDate;
  private Date endDate;
  private boolean active;
  private String rulesJson;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Date getStartDate() {
    return startDate;
  }

  public void setStartDate(Date startDate) {
    this.startDate = startDate;
  }

  public Date getEndDate() {
    return endDate;
  }

  public void setEndDate(Date endDate) {
    this.endDate = endDate;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public String getRulesJson() {
    return rulesJson;
  }

  public void setRulesJson(String rulesJson) {
    this.rulesJson = rulesJson;
  }
}
