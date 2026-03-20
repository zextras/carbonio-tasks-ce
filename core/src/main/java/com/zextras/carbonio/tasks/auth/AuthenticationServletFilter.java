// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.auth;

import com.google.inject.Inject;
import com.zextras.carbonio.tasks.Constants.Config;
import com.zextras.carbonio.tasks.Constants.GraphQL.Context;
import com.zextras.carbonio.user_management.sdk.grpc.GetUserMyselfRequest;
import com.zextras.carbonio.user_management.sdk.grpc.UserManagementServiceGrpc.UserManagementServiceBlockingStub;
import com.zextras.carbonio.user_management.sdk.grpc.UserMyselfProto;
import com.zextras.carbonio.user_management.sdk.grpc.UserMyselfResponse;
import com.zextras.carbonio.user_management.sdk.grpc.UserTypeProto;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthenticationServletFilter implements Filter {

  private static final Logger logger = LoggerFactory.getLogger(AuthenticationServletFilter.class);

  private final UserManagementServiceBlockingStub userManagementStub;

  @Inject
  public AuthenticationServletFilter(UserManagementServiceBlockingStub userManagementStub) {
    this.userManagementStub = userManagementStub;
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
        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return;
      }

      String token = optZmCookie.get().getValue();

      try {
        GetUserMyselfRequest grpcRequest =
            GetUserMyselfRequest.newBuilder().setToken(token).build();
        UserMyselfResponse grpcResponse = userManagementStub.getUserMyself(grpcRequest);
        UserMyselfProto userMyself = grpcResponse.getUser();

        if (userMyself.getInfo().getType() == UserTypeProto.GUEST) {
          logger.error("The request is unauthorized: the user is not an internal one");
          httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          return;
        }

        if (!userMyself.getInfo().getStatus().equalsIgnoreCase("active")) {
          logger.error("The request is unauthorized: the user is not active");
          httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          return;
        }

        if (!userMyself.getFeaturesList().contains("carbonioFeatureTasksEnabled")) {
          logger.error("The request is unauthorized: the user is not an internal one");
          httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          return;
        }

        httpRequest.setAttribute(Context.REQUESTER_ID, userMyself.getInfo().getUserId());
        filterChain.doFilter(httpRequest, httpResponse);

      } catch (StatusRuntimeException e) {
        if (e.getStatus().getCode() == Status.Code.UNAUTHENTICATED) {
          logger.error("The request is unauthorized: the cookie is invalid");
          httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
          logger.error("User management service call failed: {}", e.getMessage());
          httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
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
