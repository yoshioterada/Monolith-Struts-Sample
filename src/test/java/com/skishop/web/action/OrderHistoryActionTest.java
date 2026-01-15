package com.skishop.web.action;

import com.skishop.domain.user.User;

public class OrderHistoryActionTest extends StrutsActionTestBase {
  public void testOrderHistoryRequiresLogin() throws Exception {
    setRequestPathInfo("/orders");
    actionPerform();
    verifyForward("login");
  }

  public void testOrderHistorySuccess() throws Exception {
    User user = new User();
    user.setId("u-1");
    getSession().setAttribute("loginUser", user);
    setRequestPathInfo("/orders");
    actionPerform();
    verifyInputForward();
    assertNotNull(getRequest().getAttribute("orders"));
  }
}
