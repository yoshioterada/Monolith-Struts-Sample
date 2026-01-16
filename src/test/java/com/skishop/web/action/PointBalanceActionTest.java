package com.skishop.web.action;

import com.skishop.domain.point.PointAccount;
import servletunit.HttpServletResponseSimulator;

public class PointBalanceActionTest extends StrutsActionTestBase {
  public void testPointBalanceRequiresLogin() throws Exception {
    setRequestPathInfo("/points");
    setGetRequest();
    actionPerform();
    HttpServletResponseSimulator response = (HttpServletResponseSimulator) getResponse();
    assertEquals(302, response.getStatusCode());
    assertEquals("/login.do", response.getHeader("Location"));
  }

  public void testPointBalanceSuccess() throws Exception {
    setLoginUser("u-1", "USER");
    setRequestPathInfo("/points");
    setGetRequest();
    actionPerform();
    verifyInputForward();
    PointAccount account = (PointAccount) getRequest().getAttribute("pointBalance");
    assertNotNull(account);
    assertEquals("u-1", account.getUserId());
  }
}
