package com.skishop.web.action;

import com.skishop.common.util.PasswordHasher;
import com.skishop.dao.user.SecurityLogDao;
import com.skishop.dao.user.SecurityLogDaoImpl;
import com.skishop.dao.user.UserDao;
import com.skishop.dao.user.UserDaoImpl;
import com.skishop.domain.user.User;

public class LoginActionTest extends StrutsActionTestBase {
  public void testLoginValidation() throws Exception {
    setRequestPathInfo("/login");
    setPostRequest();
    addRequestParameter("email", "");
    addRequestParameter("password", "");
    actionPerform();
    verifyInputForward();
    verifyActionErrors(new String[] {"errors.required", "errors.required"});
  }

  public void testLoginSuccess() throws Exception {
    UserDao userDao = new UserDaoImpl();
    String salt = PasswordHasher.generateSalt();
    String hash = PasswordHasher.hash("password123", salt);
    userDao.updatePassword("u-1", hash, salt);

    setRequestPathInfo("/login");
    setPostRequest();
    addRequestParameter("email", "user@example.com");
    addRequestParameter("password", "password123");
    actionPerform();
    verifyForward("success");
    assertNotNull(getRequest().getSession().getAttribute("loginUser"));
  }

  public void testLoginLockoutAfterFailures() throws Exception {
    UserDao userDao = new UserDaoImpl();
    SecurityLogDao securityLogDao = new SecurityLogDaoImpl();
    String salt = PasswordHasher.generateSalt();
    String hash = PasswordHasher.hash("password123", salt);
    userDao.updatePassword("u-1", hash, salt);

    for (int i = 0; i < 5; i++) {
      setRequestPathInfo("/login");
      setPostRequest();
      addRequestParameter("email", "user@example.com");
      addRequestParameter("password", "wrongpass");
      actionPerform();
      verifyForward("failure");
    }

    User user = userDao.findById("u-1");
    assertEquals("LOCKED", user.getStatus());
    assertEquals(5, securityLogDao.countByUserAndEvent("u-1", "LOGIN_FAILURE"));
    assertEquals(1, securityLogDao.countByUserAndEvent("u-1", "ACCOUNT_LOCKED"));
  }
}
