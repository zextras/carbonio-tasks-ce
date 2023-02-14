// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks;

import com.google.inject.Inject;
import com.zextras.carbonio.tasks.graphql.GraphQLServlet;
import com.zextras.carbonio.tasks.rest.RestApplication;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;

public class JettyServer {

  private final GraphQLServlet graphQLServlet;

  @Inject
  public JettyServer(GraphQLServlet graphQLServlet) {
    this.graphQLServlet = graphQLServlet;
  }

  /**
   * Creates a new jetty {@link Server} instance with two servlets:
   *
   * <ul>
   *   <li>{@link GraphQLServlet}: necessary to handle the GraphQL requests
   *   <li>{@link HttpServlet30Dispatcher}: necessary to handle the REST requests such as the health
   *       APIs
   * </ul>
   *
   * @throws Exception if something goes wrong during the stop of the server
   */
  public void start() throws Exception {

    // Unfortunately the jetty server is not AutoClosable, so I can't use try-with-resources
    Server server = null;
    try {
      server = new Server();
      try (ServerConnector connector = new ServerConnector(server)) {
        connector.setDefaultProtocol("HTTP/1.1");
        connector.setHost("127.78.0.16");
        connector.setPort(10_000);
        server.addConnector(connector);
      }
      // Create a ContextHandlerCollection to hold multiple context handlers.
      ContextHandlerCollection contextCollection = new ContextHandlerCollection();
      contextCollection.addHandler(createGraphQLHandler());
      contextCollection.addHandler(createRESTHandler());
      server.setHandler(contextCollection);

      server.start();
      server.join();

    } finally {
      if (server != null) {
        server.stop();
      }
    }
  }

  /**
   * @return an {@link ServletContextHandler} associated to a {@link GraphQLServlet} instance.
   */
  private Handler createGraphQLHandler() {
    ServletContextHandler servletContextHandler = new ServletContextHandler();
    servletContextHandler.setContextPath("/graphql/");
    ServletHolder graphQLServletHolder = new ServletHolder("graphql-servlet", graphQLServlet);

    servletContextHandler.addServlet(graphQLServletHolder, "/");
    return servletContextHandler;
  }

  /**
   * @return an {@link ServletContextHandler} associated to a {@link HttpServlet30Dispatcher}
   *     instance.
   */
  private Handler createRESTHandler() {
    ServletContextHandler servletContextHandler = new ServletContextHandler();
    servletContextHandler.setContextPath("/rest/");
    ServletHolder servletHolder =
        servletContextHandler.addServlet(HttpServlet30Dispatcher.class, "/*");
    servletHolder.setInitParameter("javax.ws.rs.Application", RestApplication.class.getName());
    return servletContextHandler;
  }
}
