// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.rest.services;

import com.google.inject.Inject;
import com.zextras.carbonio.tasks.dal.DatabaseManager;
import com.zextras.carbonio.tasks.rest.types.health.DependencyType;
import com.zextras.carbonio.tasks.rest.types.health.HealthStatus;
import com.zextras.carbonio.tasks.rest.types.health.ServiceHealth;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import java.util.ArrayList;
import java.util.List;


public class HealthService {

  private final DatabaseManager databaseManager;
  private final ManagedChannel userManagementChannel;

  @Inject
  public HealthService(DatabaseManager databaseManager, ManagedChannel userManagementChannel) {
    this.databaseManager = databaseManager;
    this.userManagementChannel = userManagementChannel;
  }

  public boolean areServiceDependenciesReady() {
    return databaseManager.isDatabaseLive() && isUserManagementAlive();
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
    boolean databaseIsReady = databaseIsLive ? databaseManager.isDatabaseCorrectVersion() : false;

    return new ServiceHealth()
        .setName("database")
        .setType(DependencyType.REQUIRED)
        .setLive(databaseIsLive)
        .setReady(databaseIsReady);
  }

  public ServiceHealth getUserManagementHealth() {
    boolean userManagementIsLive = isUserManagementAlive();

    return new ServiceHealth()
        .setName("carbonio-user-management")
        .setType(DependencyType.REQUIRED)
        .setLive(userManagementIsLive)
        .setReady(userManagementIsLive);
  }

  private boolean isUserManagementAlive() {
    ConnectivityState state = userManagementChannel.getState(true);
    return state == ConnectivityState.READY || state == ConnectivityState.IDLE;
  }
}
