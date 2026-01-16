package com.skishop.web.action;

import com.skishop.domain.user.User;
import com.skishop.service.auth.AuthResult;
import com.skishop.service.auth.AuthService;
import com.skishop.web.form.LoginForm;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

public class LoginAction extends Action {
  private final AuthService authService = new AuthService();

  public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    if (!"POST".equalsIgnoreCase(request.getMethod())) {
      return mapping.getInputForward();
    }
    LoginForm loginForm = (LoginForm) form;
    AuthResult result = authService.authenticate(loginForm.getEmail(), loginForm.getPassword(), request.getRemoteAddr(),
        request.getHeader("User-Agent"));
    if (!result.isSuccess()) {
      ActionMessages errors = new ActionMessages();
      errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.login.failed"));
      saveErrors(request, errors);
      return mapping.findForward("failure");
    }
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    session = request.getSession(true);
    User user = result.getUser();
    session.setAttribute("loginUser", user);
    return mapping.findForward("success");
  }
}
