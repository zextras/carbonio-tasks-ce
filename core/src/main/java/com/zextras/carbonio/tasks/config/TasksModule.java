// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.config;

import com.google.inject.AbstractModule;
import com.zextras.carbonio.tasks.dal.repositories.DbInfoRepository;
import com.zextras.carbonio.tasks.dal.repositories.TaskRepository;
import com.zextras.carbonio.tasks.dal.repositories.impl.DbInfoRepositoryEbean;
import com.zextras.carbonio.tasks.dal.repositories.impl.TaskRepositoryEbean;
import com.zextras.carbonio.tasks.rest.controllers.HealthController;
import com.zextras.carbonio.tasks.rest.controllers.HealthControllerImpl;
import java.time.Clock;

public class TasksModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(Clock.class).toInstance(Clock.systemUTC());
    bind(HealthController.class).to(HealthControllerImpl.class);
    bind(DbInfoRepository.class).to(DbInfoRepositoryEbean.class);
    bind(TaskRepository.class).to(TaskRepositoryEbean.class);
  }
}
