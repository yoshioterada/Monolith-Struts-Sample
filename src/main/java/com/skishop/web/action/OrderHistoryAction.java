package com.skishop.web.action;

import com.skishop.domain.order.Order;
import com.skishop.domain.user.User;
import com.skishop.service.order.OrderService;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

public class OrderHistoryAction extends Action {
  private static final Log logger = LogFactory.getLog(OrderHistoryAction.class);
  private final OrderService orderService = new OrderService();

  public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    HttpSession session = request.getSession(false);
    User user = session != null ? (User) session.getAttribute("loginUser") : null;
    if (user == null) {
      return mapping.findForward("login");
    }
    List<Order> orders;
    if (user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole())) {
      orders = orderService.listAll(50);
    } else {
      orders = orderService.listByUserId(user.getId());
    }
    System.out.println("[OrderHistoryAction] user=" + user.getEmail() + " role=" + user.getRole() + " orders=" + (orders != null ? orders.size() : null));
    request.setAttribute("orders", orders);
    return mapping.getInputForward();
  }
}
