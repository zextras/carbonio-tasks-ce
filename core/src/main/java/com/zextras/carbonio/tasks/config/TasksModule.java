// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;
import com.zextras.carbonio.tasks.Constants.Config.UserService;
import com.zextras.carbonio.tasks.Constants.Service.API.Endpoints;
import com.zextras.carbonio.tasks.auth.AuthenticationServletFilter;
import com.zextras.carbonio.tasks.dal.impl.DatabaseFlywayInitializer;
import com.zextras.carbonio.tasks.dal.DatabaseInitializer;
import com.zextras.carbonio.tasks.dal.repositories.DbInfoRepository;
import com.zextras.carbonio.tasks.dal.repositories.TaskRepository;
import com.zextras.carbonio.tasks.dal.repositories.impl.DbInfoRepositoryFlyway;
import com.zextras.carbonio.tasks.dal.repositories.impl.TaskRepositoryEbean;
import com.zextras.carbonio.tasks.graphql.GraphQLServlet;
import com.zextras.carbonio.tasks.rest.RestApplication;
import com.zextras.carbonio.tasks.rest.controllers.HealthController;
import com.zextras.carbonio.tasks.rest.controllers.HealthControllerImpl;
import com.zextras.carbonio.usermanagement.UserManagementClient;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

import org.flywaydb.core.Flyway;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;

public class TasksModule extends AbstractModule {

  @Override
  protected void configure() {
    // Be aware that the binding of the all servlet objects and the RequestScoped annotation
    // are done in the GuiceFilter InternalServletModule. It is used when the ServletModule is
    // instantiated (see below)

    bind(Clock.class).toInstance(Clock.systemUTC());
    bind(HealthController.class).to(HealthControllerImpl.class);
    bind(DbInfoRepository.class).to(DbInfoRepositoryFlyway.class);
    bind(TaskRepository.class).to(TaskRepositoryEbean.class);
    bind(DatabaseInitializer.class).to(DatabaseFlywayInitializer.class);
    bind(Flyway.class).in(Singleton.class);

    install(
        new ServletModule() {
          @Override
          protected void configureServlets() {
            bind(ResteasyJackson2Provider.class);
            bind(GraphQLServlet.class).in(Singleton.class);
            bind(HttpServlet30Dispatcher.class).in(Singleton.class);
            bind(AuthenticationServletFilter.class).in(Singleton.class);

            filter(Endpoints.GRAPHQL).through(AuthenticationServletFilter.class);
            serve(Endpoints.GRAPHQL).with(GraphQLServlet.class);

            Map<String, String> initParam = new HashMap<>();
            initParam.put("jakarta.ws.rs.core.Application", RestApplication.class.getName());
            initParam.put("resteasy.servlet.mapping.prefix", Endpoints.REST);
            serve(Endpoints.REST + "/*").with(HttpServlet30Dispatcher.class, initParam);
          }
        });
  }

  @Provides
  public UserManagementClient getUserManagementClient() {
    return UserManagementClient.atURL(UserService.PROTOCOL, UserService.URL, UserService.PORT);
  }
}
