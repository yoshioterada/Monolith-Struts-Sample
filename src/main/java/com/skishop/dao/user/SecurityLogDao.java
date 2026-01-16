package com.skishop.dao.user;

import com.skishop.domain.user.SecurityLog;

public interface SecurityLogDao {
  void insert(SecurityLog log);

  int countByUserAndEvent(String userId, String eventType);
}
