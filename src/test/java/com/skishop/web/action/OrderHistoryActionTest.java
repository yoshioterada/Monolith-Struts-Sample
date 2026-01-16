package com.skishop.web.action;

import servletunit.HttpServletResponseSimulator;

public class OrderHistoryActionTest extends StrutsActionTestBase {
  public void testOrderHistoryRequiresLogin() throws Exception {
    setRequestPathInfo("/orders");
    setGetRequest();
    actionPerform();
    HttpServletResponseSimulator response = (HttpServletResponseSimulator) getResponse();
    assertEquals(302, response.getStatusCode());
    assertEquals("/login.do", response.getHeader("Location"));
  }

  public void testOrderHistorySuccess() throws Exception {
    setLoginUser("u-1", "USER");
    setRequestPathInfo("/orders");
    setGetRequest();
    actionPerform();
    verifyInputForward();
    assertNotNull(getRequest().getAttribute("orders"));
  }
}
