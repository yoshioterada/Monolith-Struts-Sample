package com.skishop.web.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

public class RequestIdFilter implements Filter {
  private static final String HEADER_NAME = "X-Request-Id";
  private static final String MDC_KEY = "reqId";

  @Override
  public void init(FilterConfig filterConfig) {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    String requestId = null;
    if (request instanceof HttpServletRequest httpRequest) {
      requestId = httpRequest.getHeader(HEADER_NAME);
    }
    if (isBlank(requestId)) {
      requestId = UUID.randomUUID().toString();
    }
    MDC.put(MDC_KEY, requestId);
    if (response instanceof HttpServletResponse httpResponse) {
      httpResponse.setHeader(HEADER_NAME, requestId);
    }
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }

  @Override
  public void destroy() {
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
