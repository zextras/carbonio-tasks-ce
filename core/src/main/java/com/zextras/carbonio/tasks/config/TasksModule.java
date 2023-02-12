// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.config;

import com.google.inject.AbstractModule;
import com.zextras.carbonio.tasks.controllers.HealthController;
import com.zextras.carbonio.tasks.controllers.HealthControllerImpl;

public class TasksModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(HealthController.class).to(HealthControllerImpl.class);
  }
}
