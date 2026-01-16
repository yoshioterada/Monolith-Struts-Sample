package com.skishop.web.action;

import com.skishop.domain.address.Address;
import java.util.List;
import servletunit.HttpServletResponseSimulator;

public class AddressListActionTest extends StrutsActionTestBase {
  public void testAddressListRequiresLogin() throws Exception {
    setRequestPathInfo("/addresses");
    setGetRequest();
    actionPerform();
    HttpServletResponseSimulator response = (HttpServletResponseSimulator) getResponse();
    assertEquals(302, response.getStatusCode());
    assertEquals("/login.do", response.getHeader("Location"));
  }

  public void testAddressListSuccess() throws Exception {
    setLoginUser("u-1", "USER");
    setRequestPathInfo("/addresses");
    setGetRequest();
    actionPerform();
    verifyInputForward();
    List<Address> addresses = (List<Address>) getRequest().getAttribute("addresses");
    assertNotNull(addresses);
    assertFalse(addresses.isEmpty());
    assertEquals("u-1", addresses.get(0).getUserId());
  }
}
