package com.skishop.web.action;

import com.skishop.domain.cart.Cart;
import com.skishop.domain.cart.CartItem;
import com.skishop.domain.user.User;
import com.skishop.service.cart.CartService;
import com.skishop.web.form.AddCartForm;
import java.math.BigDecimal;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

public class CartAction extends Action {
  private static final int CART_COOKIE_MAX_AGE = 30 * 24 * 60 * 60; // 30 days in seconds
  private final CartService cartService = new CartService();

  public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    HttpSession session = request.getSession(true);
    String cartId = (String) session.getAttribute("cartId");
    if (cartId == null) {
      User user = (User) session.getAttribute("loginUser");
      Cart cart = cartService.createCart(user != null ? user.getId() : null, session.getId());
      cartId = cart.getId();
      session.setAttribute("cartId", cartId);
      addCartCookie(request, response, cartId);
    }

    if ("POST".equalsIgnoreCase(request.getMethod())) {
      AddCartForm cartForm = (AddCartForm) form;
      if (cartForm != null && cartForm.getProductId() != null && cartForm.getProductId().length() > 0) {
        cartService.addItem(cartId, cartForm.getProductId(), cartForm.getQuantity());
      }
    }

    List<CartItem> items = cartService.getItems(cartId);
    BigDecimal subtotal = cartService.calculateSubtotal(items);
    request.setAttribute("cartItems", items);
    request.setAttribute("cartSubtotal", subtotal);
    request.setAttribute("cartId", cartId);
    return mapping.findForward("success");
  }

  private void addCartCookie(HttpServletRequest request, HttpServletResponse response, String cartId) {
    String path = request.getContextPath();
    if (path == null || path.length() == 0) {
      path = "/";
    }
    StringBuilder header = new StringBuilder();
    header.append("CART_ID=").append(cartId);
    header.append("; Max-Age=").append(CART_COOKIE_MAX_AGE);
    header.append("; Path=").append(path);
    if (request.isSecure()) {
      header.append("; Secure");
    }
    header.append("; HttpOnly");
    response.addHeader("Set-Cookie", header.toString());
  }
}
