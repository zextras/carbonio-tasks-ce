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
 * Represents a {@link javax.servlet.http.HttpServlet} for the GraphQL endpoint with a configuration
 * containing:
 *
 * <ul>
 *   <li>The GraphQL SDL schema loaded from resources
 *   <li>The Wiring to bind the queries and mutations to the related data-fetchers
 *   <li>An instrumentation necessary for the input validation
 * </ul>
 */
public class GraphQLServlet extends GraphQLHttpServlet {

  private final GraphQLProvider graphQLProvider;

  @Inject
  public GraphQLServlet(GraphQLProvider graphQLProvider) {
    this.graphQLProvider = graphQLProvider;
  }

  /**
   * @return a {@link GraphQLConfiguration} containing a {@link graphql.schema.GraphQLSchema} and a
   *     {@link graphql.execution.instrumentation.fieldvalidation.FieldValidationInstrumentation}
   *     necessary to handle http requests.
   */
  @Override
  protected GraphQLConfiguration getConfiguration() {
    GraphQLQueryInvoker queryInvoker =
        GraphQLQueryInvoker.newBuilder()
            .with(
                Arrays.asList(
                    graphQLProvider.buildValidationInstrumentation(),
                    graphQLProvider.getContextInstrumentation()))
            .build();

    return GraphQLConfiguration.with(graphQLProvider.buildSchema()).with(queryInvoker).build();
  }
}
