// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.rest.services;

import com.google.inject.Inject;
import com.zextras.carbonio.tasks.config.TasksConfig;
import com.zextras.carbonio.tasks.dal.repositories.DbInfoRepository;
import com.zextras.carbonio.tasks.rest.types.health.DependencyType;
import com.zextras.carbonio.tasks.rest.types.health.HealthStatus;
import com.zextras.carbonio.tasks.rest.types.health.ServiceHealth;
import com.zextras.carbonio.usermanagement.UserManagementClient;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;

import java.util.ArrayList;
import java.util.List;

public class HealthService {

  private final DbInfoRepository dbInfoRepository;
  private final UserManagementClient userManagementClient;

  private final TasksConfig tasksConfig;

  @Inject
  public HealthService(
          DbInfoRepository dbInfoRepository, UserManagementClient userManagementClient, TasksConfig tasksConfig) {
    this.dbInfoRepository = dbInfoRepository;
    this.userManagementClient = userManagementClient;
    this.tasksConfig = tasksConfig;
  }

  public boolean areServiceDependenciesReady() {
    return dbInfoRepository.isDatabaseLive() && userManagementClient.healthCheck();
  }

  public HealthStatus getServiceHealthStatus() {
    List<ServiceHealth> dependencies = new ArrayList<>();
    dependencies.add(getDatabaseHealth());
    dependencies.add(getUserManagementHealth());

    boolean tasksIsReady =
        dependencies.stream()
            .filter(dependency -> DependencyType.REQUIRED.equals(dependency.getType()))
            .allMatch(ServiceHealth::isReady);

    return new HealthStatus().setReady(tasksIsReady).setDependencies(dependencies);
  }

  public ServiceHealth getDatabaseHealth() {
    boolean databaseIsLive = dbInfoRepository.isDatabaseLive();
    //has dbflywayinitializer.initialize already been called here?
    //in other words can i assume flyway migration has already happened?

    Flyway flyway = Flyway.configure()
            .dataSource(tasksConfig.getDataSource())
            .load();

    //flyway resolver will return an empty physical path if the last database version's script is not included in
    //local migration path (db/migration). if the last version's script is not in path the database was migrated
    //to a newer version by another client and this client must be updated
    boolean clientIsUpdated = !flyway.info().current().getPhysicalLocation().isEmpty();

    return new ServiceHealth()
        .setName("database")
        .setType(DependencyType.REQUIRED)
        .setLive(databaseIsLive && clientIsUpdated)
        .setReady(databaseIsLive && clientIsUpdated);
  }

  public ServiceHealth getUserManagementHealth() {
    boolean userManagementIsLive = userManagementClient.healthCheck();

    return new ServiceHealth()
        .setName("carbonio-user-management")
        .setType(DependencyType.REQUIRED)
        .setLive(userManagementIsLive)
        .setReady(userManagementIsLive);
  }
}
