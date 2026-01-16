package com.skishop.web.action;

public class CheckoutActionTest extends StrutsActionTestBase {
  public void testCheckoutSuccessForward() throws Exception {
    setLoginUser("u-1", "USER");
    setRequestPathInfo("/checkout");
    setPostRequest();
    addRequestParameter("cartId", "cart-1");
    addRequestParameter("paymentMethod", "CARD");
    addRequestParameter("cardNumber", "4111111111111111");
    addRequestParameter("cardExpMonth", "12");
    addRequestParameter("cardExpYear", "2099");
    addRequestParameter("cardCvv", "123");
    addRequestParameter("billingZip", "12345");
    actionPerform();
    verifyForward("success");
    assertNotNull(getRequest().getAttribute("order"));
  }
}
