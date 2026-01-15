package com.skishop.web.action;

public class CouponApplyActionTest extends StrutsActionTestBase {
  public void testCouponApplyValidation() throws Exception {
    setRequestPathInfo("/coupon/apply");
    setPostRequest();
    addRequestParameter("code", "A");
    actionPerform();
    verifyInputForward();
    verifyActionErrors(new String[] {"errors.minlength"});
  }

  public void testCouponApplyMissingCart() throws Exception {
    setRequestPathInfo("/coupon/apply");
    setPostRequest();
    addRequestParameter("code", "SAVE10");
    actionPerform();
    verifyForward("failure");
    verifyActionErrors(new String[] {"error.cart.notfound"});
  }
}
