package com.skishop.web.processor;

import com.skishop.domain.user.User;
import java.io.IOException;
import java.util.StringTokenizer;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.TilesRequestProcessor;
import org.apache.struts.util.TokenProcessor;

public class AuthRequestProcessor extends TilesRequestProcessor {
  private static final String LOGIN_PATH = "/login.do";
  private static final String LOGIN_USER_KEY = "loginUser";

  protected boolean processPreprocess(HttpServletRequest request, HttpServletResponse response) {
    if (!super.processPreprocess(request, response)) {
      return false;
    }
    if (!"POST".equalsIgnoreCase(request.getMethod())) {
      TokenProcessor.getInstance().saveToken(request);
    }
    return true;
  }

  protected boolean processRoles(HttpServletRequest request, HttpServletResponse response, ActionMapping mapping)
      throws IOException, ServletException {
    String roles = mapping.getRoles();
    if (roles == null || roles.trim().length() == 0) {
      return true;
    }
    User user = getLoginUser(request);
    if (user == null) {
      redirectToLogin(request, response);
      return false;
    }
    if (!hasRole(user, roles)) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return false;
    }
    return true;
  }

  protected ActionForward processActionPerform(HttpServletRequest request, HttpServletResponse response, Action action,
      ActionForm form, ActionMapping mapping) throws IOException, ServletException {
    if (requiresTokenValidation(request)) {
      TokenProcessor processor = TokenProcessor.getInstance();
      if (!processor.isTokenValid(request)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return null;
      }
      processor.resetToken(request);
    }
    return super.processActionPerform(request, response, action, form, mapping);
  }

  private boolean requiresTokenValidation(HttpServletRequest request) {
    return "POST".equalsIgnoreCase(request.getMethod());
  }

  private User getLoginUser(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    return session != null ? (User) session.getAttribute(LOGIN_USER_KEY) : null;
  }

  private boolean hasRole(User user, String roles) {
    if (user == null || user.getRole() == null) {
      return false;
    }
    StringTokenizer tokenizer = new StringTokenizer(roles, ",");
    while (tokenizer.hasMoreTokens()) {
      String role = tokenizer.nextToken().trim();
      if (role.length() > 0 && role.equalsIgnoreCase(user.getRole())) {
        return true;
      }
    }
    return false;
  }

  private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String contextPath = request.getContextPath();
    if (contextPath == null) {
      contextPath = "";
    }
    response.sendRedirect(contextPath + LOGIN_PATH);
  }
}
