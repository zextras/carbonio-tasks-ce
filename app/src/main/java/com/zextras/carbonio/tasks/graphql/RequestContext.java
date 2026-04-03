// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql;

import jakarta.enterprise.context.RequestScoped;

/**
 * Request-scoped CDI bean used to propagate authentication state from the JAX-RS
 * {@link com.zextras.carbonio.tasks.auth.AuthenticationFilter} to the SmallRye GraphQL API layer.
 *
 * <p>The filter sets {@link #requesterId} after a successful gRPC validation; the GraphQL API
 * reads it to scope queries and mutations to the authenticated user.
 */
@RequestScoped
public class RequestContext {

  private String requesterId;

  public String getRequesterId() {
    return requesterId;
  }

  public void setRequesterId(String requesterId) {
    this.requesterId = requesterId;
  }
}
