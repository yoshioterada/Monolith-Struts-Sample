package com.skishop.web.action;

import com.skishop.dao.user.PasswordResetTokenDao;
import com.skishop.dao.user.PasswordResetTokenDaoImpl;
import com.skishop.domain.user.PasswordResetToken;

public class PasswordForgotActionTest extends StrutsActionTestBase {
  public void testPasswordForgotGetShowsForm() throws Exception {
    setRequestPathInfo("/password/forgot");
    setGetRequest();
    actionPerform();
    verifyInputForward();
  }

  public void testPasswordForgotCreatesResetToken() throws Exception {
    PasswordResetTokenDao tokenDao = new PasswordResetTokenDaoImpl();
    setRequestPathInfo("/password/forgot");
    setPostRequest();
    addRequestParameter("email", "user@example.com");
    actionPerform();
    verifyForward("success");
    String tokenValue = (String) getRequest().getAttribute("resetToken");
    assertNotNull(tokenValue);
    PasswordResetToken token = tokenDao.findByToken(tokenValue);
    assertNotNull(token);
    assertEquals("u-1", token.getUserId());
  }
}
