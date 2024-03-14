// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal;

public interface DatabaseManager {
  void initialize();
  String getDatabaseVersion();
  boolean isDatabaseLive();
  boolean isDatabaseCorrectVersion();
}
