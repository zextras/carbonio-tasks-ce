// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.auth;

import com.google.inject.Inject;
import com.zextras.carbonio.tasks.Constants.Config;
import com.zextras.carbonio.tasks.Constants.GraphQL.Context;
import com.zextras.carbonio.usermanagement.UserManagementClient;
import com.zextras.carbonio.usermanagement.entities.UserId;
import io.vavr.control.Try;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthenticationServletFilter implements Filter {

  private static final Logger logger = LoggerFactory.getLogger(AuthenticationServletFilter.class);

  private final UserManagementClient userManagementClient;

  @Inject
  public AuthenticationServletFilter(UserManagementClient userManagementClient) {
    this.userManagementClient = userManagementClient;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    Filter.super.init(filterConfig);
    logger.info(
        "Filter initialized to {} endpoint",
        filterConfig.getServletContext().getServletContextName());
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;

      Optional<Cookie> optZmCookie =
          Arrays.stream(httpRequest.getCookies())
              .filter(cookie -> Config.ACCEPTED_COOKIE_TYPE.equals(cookie.getName()))
              .findFirst();

      if (optZmCookie.isEmpty()) {
        logger.error("The request is unauthorized: the cookie is missing");
        httpResponse.setStatus(HttpStatus.SC_UNAUTHORIZED);
        return;
      }

      // Unfortunately doFilter throws an exception, using the .map would lead to an unreadable code
      Try<UserId> tryUserId = userManagementClient.validateUserToken(optZmCookie.get().getValue());

      if (tryUserId.isSuccess()) {
        httpRequest.setAttribute(Context.REQUESTER_ID, tryUserId.get().getUserId());
        filterChain.doFilter(httpRequest, httpResponse);
      } else {
        logger.error("The request is unauthorized: the cookie is invalid");
        httpResponse.setStatus(HttpStatus.SC_UNAUTHORIZED);
      }
    } else {
      logger.error("Unable to authenticate non HTTP requests");
    }
  }

  @Override
  public void destroy() {
    logger.trace("The destroy of this filter is not necessary");
  }
}
