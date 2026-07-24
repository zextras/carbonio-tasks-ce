// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.auth;

import com.zextras.carbonio.tasks.Constants.Config;
import com.zextras.carbonio.tasks.Constants.GraphQL.Context;
import com.zextras.carbonio.user_management.sdk.rest.ApiException;
import com.zextras.carbonio.user_management.sdk.rest.api.UserResourceApi;
import com.zextras.carbonio.user_management.sdk.rest.model.MyselfDto;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vert.x route handler that:
 *
 * <ol>
 *   <li>Runs on every HTTP request.
 *   <li>Skips requests that are NOT targeting {@code /graphql} (e.g., health endpoints).
 *   <li>Extracts the {@code ZM_AUTH_TOKEN} cookie and validates it against carbonio-user-management
 *       via its REST {@code /internal/users/myself} endpoint.
 *   <li>On success, stores the {@code requesterId} in the Vert.x {@link RoutingContext} so the
 *       request-scoped {@link com.zextras.carbonio.tasks.graphql.RequestContext} can expose it to
 *       GraphQL data-fetchers.
 * </ol>
 *
 * <p>Registered via CDI observer on {@link Router} with priority 100, which runs before the
 * SmallRye GraphQL handler at order 1000.
 */
@ApplicationScoped
public class AuthenticationFilter {

  private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

  @Inject
  UserResourceApi userResourceApi;

  /**
   * Registers the auth handler on the Vert.x router. Called once at startup when Quarkus publishes
   * the {@link Router} CDI event.
   */
  public void registerRoutes(@Observes Router router) {
    // blockingHandler ensures the REST call is NOT made from the event loop
    router.route("/graphql").order(-100).blockingHandler(this::filter);
    router.route("/graphql/").order(-100).blockingHandler(this::filter);
  }

  /**
   * Core auth logic. Called for every request matching {@code /graphql} or {@code /graphql/}.
   * Sets {@link Context#REQUESTER_ID} in the routing context on success. Ends the response with
   * HTTP 401 if the request is not authenticated (missing/invalid cookie), or HTTP 403 if the
   * user is authenticated but not entitled to use Tasks (guest, inactive, or feature disabled).
   */
  void filter(RoutingContext ctx) {
    io.vertx.core.http.Cookie zmCookie = ctx.request().getCookie(Config.ACCEPTED_COOKIE_TYPE);

    if (zmCookie == null) {
      logger.error("The request is unauthorized: the ZM_AUTH_TOKEN cookie is missing");
      ctx.response().setStatusCode(401).end();
      return;
    }

    String token = zmCookie.getValue();

    try {
      MyselfDto userMyself = userResourceApi.internalUsersMyselfGet(token);

      if ("GUEST".equalsIgnoreCase(userMyself.getInfo().getType())) {
        logger.error("The request is forbidden: the user is a guest");
        ctx.response().setStatusCode(403).end();
        return;
      }

      if (!"active".equalsIgnoreCase(userMyself.getInfo().getStatus())) {
        logger.error("The request is forbidden: the user is not active");
        ctx.response().setStatusCode(403).end();
        return;
      }

      if (!userMyself.getFeatures().contains("carbonioFeatureTasksEnabled")) {
        logger.error("The request is forbidden: the user does not have Tasks feature enabled");
        ctx.response().setStatusCode(403).end();
        return;
      }

      ctx.put(Context.REQUESTER_ID, userMyself.getInfo().getUserId());
      ctx.next();

    } catch (ApiException e) {
      if (e.getCode() == 401) {
        logger.error("The request is unauthorized: the cookie is invalid");
      } else {
        logger.error("User management service call failed: {}", e.getMessage());
      }
      ctx.response().setStatusCode(401).end();
    }
  }
}
