package com.skishop.service.auth;

import com.skishop.dao.user.SecurityLogDao;
import com.skishop.dao.user.SecurityLogDaoImpl;
import com.skishop.dao.user.UserDao;
import com.skishop.dao.user.UserDaoImpl;
import com.skishop.domain.user.SecurityLog;
import com.skishop.domain.user.User;
import com.skishop.common.util.PasswordHasher;
import java.util.UUID;

public class AuthService {
  private static final int MAX_FAILED_ATTEMPTS = 5;
  private static final String EVENT_LOGIN_FAILURE = "LOGIN_FAILURE";
  private static final String EVENT_ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
  private static final String STATUS_LOCKED = "LOCKED";
  private final UserDao userDao = new UserDaoImpl();
  private final SecurityLogDao securityLogDao = new SecurityLogDaoImpl();

  public AuthResult authenticate(String email, String passwordRaw, String ipAddress, String userAgent) {
    if (email == null || passwordRaw == null) {
      return AuthResult.failure("INVALID_INPUT");
    }
    User user = userDao.findByEmail(email);
    if (user == null) {
      return AuthResult.failure("USER_NOT_FOUND");
    }
    if (STATUS_LOCKED.equalsIgnoreCase(user.getStatus())) {
      return AuthResult.failure("USER_LOCKED");
    }
    if (!PasswordHasher.matches(passwordRaw, user.getPasswordHash(), user.getSalt())) {
      recordFailure(user, ipAddress, userAgent);
      return AuthResult.failure("INVALID_CREDENTIALS");
    }
    return AuthResult.success(user);
  }

  private void recordFailure(User user, String ipAddress, String userAgent) {
    SecurityLog failureLog = buildLog(user, EVENT_LOGIN_FAILURE, ipAddress, userAgent, "{\"reason\":\"invalid_credentials\"}");
    securityLogDao.insert(failureLog);
    int failureCount = securityLogDao.countByUserAndEvent(user.getId(), EVENT_LOGIN_FAILURE);
    if (failureCount >= MAX_FAILED_ATTEMPTS) {
      userDao.updateStatus(user.getId(), STATUS_LOCKED);
      SecurityLog lockLog = buildLog(user, EVENT_ACCOUNT_LOCKED, ipAddress, userAgent,
          "{\"reason\":\"too_many_failures\",\"attempts\":" + failureCount + "}");
      securityLogDao.insert(lockLog);
    }
  }

  private SecurityLog buildLog(User user, String eventType, String ipAddress, String userAgent, String details) {
    SecurityLog log = new SecurityLog();
    log.setId(UUID.randomUUID().toString());
    log.setUserId(user.getId());
    log.setEventType(eventType);
    log.setIpAddress(ipAddress);
    log.setUserAgent(userAgent);
    log.setDetailsJson(details);
    return log;
  }
}
