package com.skishop.web.action;

public class AddressSaveActionTest extends StrutsActionTestBase {
  public void testAddressSaveRequiresLogin() throws Exception {
    setRequestPathInfo("/addresses/save");
    setPostRequest();
    addRequestParameter("recipientName", "Test User");
    addRequestParameter("postalCode", "123-4567");
    addRequestParameter("prefecture", "Tokyo");
    addRequestParameter("address1", "1-1");
    addRequestParameter("phone", "0312345678");
    actionPerform();
    servletunit.HttpServletResponseSimulator response = (servletunit.HttpServletResponseSimulator) getResponse();
    assertEquals(302, response.getStatusCode());
    assertEquals("/login.do", response.getHeader("Location"));
  }
}
