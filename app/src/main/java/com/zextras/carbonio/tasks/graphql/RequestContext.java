// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql;

import com.zextras.carbonio.tasks.Constants.GraphQL.Context;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

/**
 * Request-scoped CDI bean used to expose authentication state to the SmallRye GraphQL API layer.
 *
 * <p>The {@link com.zextras.carbonio.tasks.auth.AuthenticationFilter} stores the authenticated
 * user's ID in the Vert.x {@link RoutingContext} under {@link Context#REQUESTER_ID} before the
 * GraphQL data-fetcher runs. This bean reads from the routing context so that data-fetchers can
 * call {@link #getRequesterId()} without knowing the underlying transport.
 */
@RequestScoped
public class RequestContext {

  @Inject
  RoutingContext routingContext;

  public String getRequesterId() {
    return routingContext.get(Context.REQUESTER_ID);
  }

  /** For unit tests only — allows direct injection without a running Vert.x context. */
  public void setRequesterId(String requesterId) {
    routingContext.put(Context.REQUESTER_ID, requesterId);
  }
}
