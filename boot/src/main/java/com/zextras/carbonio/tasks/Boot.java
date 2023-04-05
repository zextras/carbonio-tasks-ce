// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.zextras.carbonio.tasks.config.TasksModule;
import com.zextras.carbonio.tasks.dal.DatabaseInitializer;
import org.slf4j.LoggerFactory;

public class Boot {
  private static final Logger rootLogger = (Logger) LoggerFactory.getLogger("root");

  public static void main(String[] args) {
    // Set configuration level
    String logLevel = System.getProperty("TASKS_LOG_LEVEL");
    rootLogger.setLevel(logLevel == null ? Level.INFO : Level.toLevel(logLevel));

    Injector injector = Guice.createInjector(new TasksModule());

    try {
      injector.getInstance(DatabaseInitializer.class).initialize();
      injector.getInstance(JettyServer.class).start();
    } catch (Exception exception) {
      rootLogger.error("Service stopped unexpectedly: " + exception.getMessage());
    }
  }
}
