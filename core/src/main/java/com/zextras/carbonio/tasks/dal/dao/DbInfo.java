// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal.dao;

import com.zextras.carbonio.tasks.Constants.Database.Tables;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Represents an Ebean {@link DbInfo} entity that matches a record of the {@link Tables#DB_INFO}
 * table.
 */
@Entity
@Table(name = Tables.DB_INFO)
public class DbInfo {

  @Column(name = Tables.DbInfo.VERSION, nullable = false)
  private int version;

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }
}
