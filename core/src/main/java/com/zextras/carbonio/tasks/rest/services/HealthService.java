// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.rest.services;

import com.google.inject.Inject;
import com.zextras.carbonio.tasks.dal.DatabaseManager;
import com.zextras.carbonio.tasks.rest.types.health.DependencyType;
import com.zextras.carbonio.tasks.rest.types.health.HealthStatus;
import com.zextras.carbonio.tasks.rest.types.health.ServiceHealth;
import com.zextras.carbonio.usermanagement.UserManagementClient;

import java.util.ArrayList;
import java.util.List;

public class HealthService {

  private final DatabaseManager databaseManager;
  private final UserManagementClient userManagementClient;

  @Inject
  public HealthService(
          DatabaseManager databaseManager, UserManagementClient userManagementClient) {
    this.databaseManager = databaseManager;
    this.userManagementClient = userManagementClient;
  }

  public boolean areServiceDependenciesReady() {
    return databaseManager.isDatabaseLive() && userManagementClient.healthCheck();
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
    boolean databaseIsLive = databaseManager.isDatabaseLive();

    //db is ready if db is live and is on correct version
    boolean databaseIsReady = databaseIsLive ? databaseManager.isDatabaseCorrectVersion() : false;

    return new ServiceHealth()
        .setName("database")
        .setType(DependencyType.REQUIRED)
        .setLive(databaseIsLive)
        .setReady(databaseIsReady);
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
