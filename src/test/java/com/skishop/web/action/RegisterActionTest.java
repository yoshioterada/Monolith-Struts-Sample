package com.skishop.web.action;

public class RegisterActionTest extends StrutsActionTestBase {
  public void testRegisterPasswordMismatch() throws Exception {
    setRequestPathInfo("/register");
    setPostRequest();
    addRequestParameter("email", "new@example.com");
    addRequestParameter("username", "newuser");
    addRequestParameter("password", "password123");
    addRequestParameter("passwordConfirm", "different123");
    actionPerform();
    verifyInputForward();
    verifyActionErrors(new String[] {"error.password.mismatch"});
  }

  public void testRegisterDuplicateEmail() throws Exception {
    setRequestPathInfo("/register");
    setPostRequest();
    addRequestParameter("email", "user@example.com");
    addRequestParameter("username", "demo");
    addRequestParameter("password", "password123");
    addRequestParameter("passwordConfirm", "password123");
    actionPerform();
    verifyForward("failure");
    verifyActionErrors(new String[] {"error.register.duplicate"});
  }
}
