// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks;

import com.google.inject.Inject;
import com.google.inject.servlet.GuiceFilter;
import com.zextras.carbonio.tasks.Constants.Service;
import com.zextras.carbonio.tasks.graphql.GraphQLServlet;
import jakarta.servlet.DispatcherType;
import java.util.EnumSet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;

public class JettyServer {

  private final GuiceResteasyBootstrapServletContextListener guiceRestEasyListener;

  @Inject
  public JettyServer(GuiceResteasyBootstrapServletContextListener guiceRestEasyListener) {
    this.guiceRestEasyListener = guiceRestEasyListener;
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
    Server server = new Server();
    try {
      try (ServerConnector connector = new ServerConnector(server)) {
        connector.setDefaultProtocol("HTTP/1.1");
        connector.setHost(Service.IP);
        connector.setPort(Service.PORT);
        server.addConnector(connector);
      }
      ServletContextHandler servletContextHandler =
          new ServletContextHandler("/", ServletContextHandler.SESSIONS);

      servletContextHandler.addFilter(GuiceFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
      servletContextHandler.addEventListener(guiceRestEasyListener);

      server.setHandler(servletContextHandler);
      server.start();
      server.join();

    } finally {
      server.stop();
    }
  }
}
