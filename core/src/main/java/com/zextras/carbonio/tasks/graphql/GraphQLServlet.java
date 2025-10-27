// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql;

import com.google.inject.Inject;
import graphql.kickstart.execution.GraphQLQueryInvoker;
import graphql.kickstart.servlet.GraphQLConfiguration;
import graphql.kickstart.servlet.GraphQLHttpServlet;
import java.util.Arrays;

/**
 * Represents a {@link jakarta.servlet.http.HttpServlet} for the GraphQL endpoint with a
 * configuration containing:
 *
 * <ul>
 *   <li>The GraphQL SDL schema loaded from resources
 *   <li>The Wiring to bind the queries and mutations to the related data-fetchers
 *   <li>An instrumentation necessary for the input validation
 * </ul>
 */
public class GraphQLServlet extends GraphQLHttpServlet {

  // Config handles threads for async operations: caching this is necessary because
  // not doing so results in a new config getting created for each request that then creates a new thread and never
  // closes it.
  private final GraphQLConfiguration cachedConfiguration;

  @Inject
  public GraphQLServlet(GraphQLProvider graphQLProvider) {
    GraphQLQueryInvoker queryInvoker =
        GraphQLQueryInvoker.newBuilder()
            .with(
                Arrays.asList(
                    graphQLProvider.buildValidationInstrumentation(),
                    graphQLProvider.getContextInstrumentation()))
            .build();

    this.cachedConfiguration = GraphQLConfiguration.with(graphQLProvider.buildSchema())
        .with(queryInvoker)
        .build();
  }

  /**
   * @return a {@link GraphQLConfiguration} containing a {@link graphql.schema.GraphQLSchema} and a
   *     {@link graphql.execution.instrumentation.fieldvalidation.FieldValidationInstrumentation}
   *     necessary to handle http requests.
   */
  @Override
  protected GraphQLConfiguration getConfiguration() {
    return cachedConfiguration;
  }
}
