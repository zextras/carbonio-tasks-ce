// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal.repositories.impl;

import com.google.inject.Inject;
import com.zextras.carbonio.tasks.dal.DatabaseConnectionManager;
import com.zextras.carbonio.tasks.dal.dao.DbInfo;
import com.zextras.carbonio.tasks.dal.repositories.DbInfoRepository;

public class DbInfoRepositoryEbean implements DbInfoRepository {

  private final DatabaseConnectionManager connectionManager;

  @Inject
  public DbInfoRepositoryEbean(DatabaseConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
  }

  @Override
  public int getDatabaseVersion() {
    return connectionManager
        .getEbeanDatabase()
        .find(DbInfo.class)
        .findOneOrEmpty()
        .map(DbInfo::getVersion)
        .orElse(0);
  }

  @Override
  public boolean isDatabaseInitialized() {
    return connectionManager.getEbeanDatabase().find(DbInfo.class).findOneOrEmpty().isPresent();
  }
}
