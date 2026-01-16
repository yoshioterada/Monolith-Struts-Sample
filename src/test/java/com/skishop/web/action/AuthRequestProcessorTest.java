package com.skishop.web.action;

import javax.servlet.http.HttpServletResponse;
import servletunit.HttpServletRequestSimulator;
import servletunit.HttpServletResponseSimulator;

public class AuthRequestProcessorTest extends StrutsActionTestBase {
  public void testCsrfFailureReturnsForbidden() throws Exception {
    setLoginUser("u-1", "USER");
    setRequestPathInfo("/checkout");
    HttpServletRequestSimulator request = (HttpServletRequestSimulator) getRequest();
    request.setMethod(HttpServletRequestSimulator.POST);
    addRequestParameter("cartId", "cart-1");
    addRequestParameter("paymentMethod", "CARD");
    addRequestParameter("cardNumber", "4111111111111111");
    addRequestParameter("cardExpMonth", "12");
    addRequestParameter("cardExpYear", "2099");
    addRequestParameter("cardCvv", "123");
    addRequestParameter("billingZip", "12345");
    try {
      actionPerform();
      fail("Expected CSRF validation to reject the request.");
    } catch (junit.framework.AssertionFailedError e) {
      // Expected error response from CSRF validation.
    }
    HttpServletResponseSimulator response = (HttpServletResponseSimulator) getResponse();
    assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatusCode());
  }
}
