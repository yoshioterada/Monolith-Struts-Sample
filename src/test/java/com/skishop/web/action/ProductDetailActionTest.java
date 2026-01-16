package com.skishop.web.action;

public class ProductDetailActionTest extends StrutsActionTestBase {
  public void testProductNotFoundForward() throws Exception {
    setRequestPathInfo("/product");
    setGetRequest();
    addRequestParameter("id", "missing");
    actionPerform();
    verifyForward("notfound");
  }
}
