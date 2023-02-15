// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.zextras.carbonio.tasks.config.TasksModule;

public class Boot {

  public static void main(String[] args) {
    Injector injector = Guice.createInjector(new TasksModule());

    try {
      injector.getInstance(JettyServer.class).start();
    } catch (Exception exception) {
      System.out.println("Service stopped unexpectedly: " + exception.getMessage());
    }
  }
}
