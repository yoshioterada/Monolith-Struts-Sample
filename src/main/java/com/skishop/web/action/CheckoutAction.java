package com.skishop.web.action;

import com.skishop.common.service.ServiceLocator;
import com.skishop.domain.order.Order;
import com.skishop.domain.user.User;
import com.skishop.service.order.OrderFacade;
import com.skishop.web.form.CheckoutForm;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

public class CheckoutAction extends Action {
  private final OrderFacade orderFacade = ServiceLocator.getOrderFacade();

  public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    if (!"POST".equalsIgnoreCase(request.getMethod())) {
      return mapping.getInputForward();
    }
    CheckoutForm checkoutForm = (CheckoutForm) form;
    HttpSession session = request.getSession(false);
    String cartId = checkoutForm.getCartId();
    if ((cartId == null || cartId.length() == 0) && session != null) {
      cartId = (String) session.getAttribute("cartId");
      checkoutForm.setCartId(cartId);
    }
    User user = session != null ? (User) session.getAttribute("loginUser") : null;
    String userId = user != null ? user.getId() : null;
    try {
      Order order = orderFacade.placeOrder(cartId, checkoutForm.getCouponCode(), checkoutForm.getUsePoints(),
          checkoutForm.toPaymentInfo(), userId);
      request.setAttribute("order", order);
      return mapping.findForward("success");
    } catch (RuntimeException e) {
      ActionMessages errors = new ActionMessages();
      errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.checkout.failed"));
      saveErrors(request, errors);
      return mapping.findForward("failure");
    }
  }
}
