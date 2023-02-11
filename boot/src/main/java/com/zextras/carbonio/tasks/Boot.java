// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.zextras.carbonio.tasks.config.TasksModule;
import java.net.InetSocketAddress;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;

public class Boot {
  public static void main(String[] args) throws Exception {
    System.out.println("Hello World!");
    Server server = null;
    Injector injector = Guice.createInjector(new TasksModule());
    try {
      server = new Server(InetSocketAddress.createUnresolved("127.78.0.16", 10_000));
      //ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
      ServletContextHandler servletHandler = new ServletContextHandler(server, "/*");

      servletHandler.addServlet(HttpServlet30Dispatcher.class, "/");
      //ServletHolder servletHolder = new ServletHolder(new HttpServlet30Dispatcher());
      //servletHolder.setInitParameter("resteasy.servlet.mapping.prefix", "/rest");
      servletHandler.setInitParameter("jakarta.ws.rs.Application",RESTTasks.class.getCanonicalName());
      //ServletHolder servletHolder = new ServletHolder("graphql-servlet", new GraphQLServlet());

      //servletHandler.addServlet(servletHolder, "/graphql/*");
      //servletHandler.addServlet(servletHolder, "/rest/*");
      //server.setHandler(servletHandler);

      server.start();
      server.join();
    } catch (Exception exception) {
      //logger.error("Service stopped unexpectedly: " + exception.getMessage(), exception);
    } finally {
      server.stop();
    }

  }
/*
public static void main(String[] args) throws Exception {
  Server server = new Server(InetSocketAddress.createUnresolved("127.78.0.16", 10000));
  server.setHandler(getHandlers());
  server.start();
  server.join();
}

  private static ServletContextHandler getHandlers() {
    ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
    handler.addServlet(HttpServletDispatcher.class, "/");
    handler.setInitParameter("jakarta.ws.rs.Application", RESTTasks.class.getCanonicalName());
    return handler;
  }

 */
}
