package com.skishop.web.action;

public class OrderCancelActionTest extends StrutsActionTestBase {
  public void testOrderCancelMissingOrder() throws Exception {
    setLoginUser("u-1", "USER");
    setRequestPathInfo("/orders/cancel");
    setPostRequest();
    actionPerform();
    verifyForward("failure");
    verifyActionErrors(new String[] {"error.order.notfound"});
  }
}
