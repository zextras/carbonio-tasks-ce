// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.auth;

import com.zextras.carbonio.tasks.Constants.Config;
import com.zextras.carbonio.tasks.clients.UserManagementClient;
import com.zextras.carbonio.tasks.graphql.RequestContext;
import com.zextras.carbonio.user_management.sdk.grpc.GetUserMyselfRequest;
import com.zextras.carbonio.user_management.sdk.grpc.UserManagementServiceGrpc.UserManagementServiceBlockingStub;
import com.zextras.carbonio.user_management.sdk.grpc.UserMyselfProto;
import com.zextras.carbonio.user_management.sdk.grpc.UserMyselfResponse;
import com.zextras.carbonio.user_management.sdk.grpc.UserTypeProto;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS {@link ContainerRequestFilter} that:
 *
 * <ol>
 *   <li>Runs on every request.
 *   <li>Skips requests that are NOT targeting {@code /graphql} (e.g., health endpoints).
 *   <li>Extracts the {@code ZM_AUTH_TOKEN} cookie and validates it against carbonio-user-management
 *       via gRPC blocking stub.
 *   <li>On success, populates the request-scoped {@link RequestContext} with the {@code requesterId}
 *       so the GraphQL data-fetchers can scope queries to the caller.
 * </ol>
 */
@Provider
@ApplicationScoped
public class AuthenticationFilter implements ContainerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

  @Inject
  UserManagementClient userManagementClient;

  @Inject
  RequestContext requestContext;

  @Override
  public void filter(ContainerRequestContext ctx) {
    String path = ctx.getUriInfo().getPath();

    // Health endpoints are intentionally unauthenticated
    if (!path.startsWith("graphql")) {
      return;
    }

    Map<String, Cookie> cookies = ctx.getCookies();
    Cookie zmCookie = cookies.get(Config.ACCEPTED_COOKIE_TYPE);

    if (zmCookie == null) {
      logger.error("The request is unauthorized: the ZM_AUTH_TOKEN cookie is missing");
      ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
      return;
    }

    String token = zmCookie.getValue();
    UserManagementServiceBlockingStub stub = userManagementClient.getBlockingStub();

    try {
      GetUserMyselfRequest grpcRequest = GetUserMyselfRequest.newBuilder().setToken(token).build();
      UserMyselfResponse grpcResponse = stub.getUserMyself(grpcRequest);
      UserMyselfProto userMyself = grpcResponse.getUser();

      if (userMyself.getInfo().getType() == UserTypeProto.GUEST) {
        logger.error("The request is unauthorized: the user is a guest");
        ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        return;
      }

      if (!userMyself.getInfo().getStatus().equalsIgnoreCase("active")) {
        logger.error("The request is unauthorized: the user is not active");
        ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        return;
      }

      if (!userMyself.getFeaturesList().contains("carbonioFeatureTasksEnabled")) {
        logger.error("The request is unauthorized: the user does not have Tasks feature enabled");
        ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        return;
      }

      requestContext.setRequesterId(userMyself.getInfo().getUserId());

    } catch (StatusRuntimeException e) {
      if (e.getStatus().getCode() == Status.Code.UNAUTHENTICATED) {
        logger.error("The request is unauthorized: the cookie is invalid");
      } else {
        logger.error("User management service call failed: {}", e.getMessage());
      }
      ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
    }
  }
}
