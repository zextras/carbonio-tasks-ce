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
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
        String.format(
            "Filter initialized to %s endpoint",
            filterConfig.getServletContext().getServletContextName()));
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
  public void destroy() {}
}
