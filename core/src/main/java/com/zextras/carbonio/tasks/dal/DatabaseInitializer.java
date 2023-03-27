// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal;

import com.google.inject.Inject;
import com.zextras.carbonio.tasks.dal.repositories.DbInfoRepository;
import java.io.FileNotFoundException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseInitializer {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

  private final DatabaseConnectionManager databaseConnectionManager;
  private final DbInfoRepository dbInfoRepository;

  @Inject
  public DatabaseInitializer(
      DatabaseConnectionManager databaseConnectionManager, DbInfoRepository dbInfoRepository) {
    this.databaseConnectionManager = databaseConnectionManager;
    this.dbInfoRepository = dbInfoRepository;
  }

  public void initialize() throws FileNotFoundException {
    // Pre-condition: having the DatabaseConnectionManager object injected, we are sure that
    // the database already exists and the credentials are working properly.
    if (!dbInfoRepository.isDatabaseInitialized()) {
      logger.info("Database not initialized. Proceed to initialize it");
      URL scriptResource = getClass().getClassLoader().getResource("sql/postgresql_1.sql");

      if (scriptResource != null) {
        databaseConnectionManager.getEbeanDatabase().script().run(scriptResource);
        logger.info("Database successfully initialized");
      } else {
        logger.error("Database not initialized due to and error when the sql script was loaded");
        throw new FileNotFoundException("Unable to load the init SQL script resource");
      }
    }
  }
}
