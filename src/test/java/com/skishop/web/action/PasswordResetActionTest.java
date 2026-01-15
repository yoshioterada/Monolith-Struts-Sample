package com.skishop.web.action;

public class PasswordResetActionTest extends StrutsActionTestBase {
  public void testPasswordResetInvalidToken() throws Exception {
    setRequestPathInfo("/password/reset");
    setPostRequest();
    addRequestParameter("token", "missing-token");
    addRequestParameter("password", "password123");
    addRequestParameter("passwordConfirm", "password123");
    actionPerform();
    verifyForward("failure");
    verifyActionErrors(new String[] {"error.password.reset.invalid"});
  }
}
