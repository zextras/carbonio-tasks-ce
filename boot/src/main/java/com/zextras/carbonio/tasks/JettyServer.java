// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks;

import com.google.inject.Inject;
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

  public void start() throws Exception {

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
    } catch (Exception exception) {
      System.out.println("Service stopped unexpectedly: " + exception.getMessage());
    } finally {
      server.stop();
    }
  }

  private Handler createGraphQLHandler() {
    ServletContextHandler servletContextHandler = new ServletContextHandler();
    servletContextHandler.setContextPath("/graphql/");
    ServletHolder graphQLServletHolder = new ServletHolder("graphql-servlet", graphQLServlet);

    servletContextHandler.addServlet(graphQLServletHolder, "/");
    return servletContextHandler;
  }

  private Handler createRESTHandler() {
    ServletContextHandler servletContextHandler = new ServletContextHandler();
    servletContextHandler.setContextPath("/rest/");
    ServletHolder servletHolder =
        servletContextHandler.addServlet(HttpServlet30Dispatcher.class, "/*");
    servletHolder.setInitParameter("javax.ws.rs.Application", RESTTasks.class.getName());
    return servletContextHandler;
  }
}
