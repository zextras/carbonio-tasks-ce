// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal.repositories;

import com.zextras.carbonio.tasks.Constants.Database.Tables;

public interface DbInfoRepository {

  /**
   * @return an <code>int</code> representing the current version of the database.
   */
  String getDatabaseVersion();

  /**
   * @return true if the database has the {@link Tables#DB_INFO} table, false otherwise.
   */

  boolean isDatabaseLive();
}
