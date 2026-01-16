package com.skishop.web.action;

public class OrderReturnActionTest extends StrutsActionTestBase {
  public void testOrderReturnMissingOrder() throws Exception {
    setLoginUser("u-1", "USER");
    setRequestPathInfo("/orders/return");
    setPostRequest();
    actionPerform();
    verifyForward("failure");
    verifyActionErrors(new String[] {"error.order.notfound"});
  }
}
