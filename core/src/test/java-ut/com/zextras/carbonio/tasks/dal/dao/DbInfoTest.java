// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal.dao;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class DbInfoTest {

  @Test
  void givenADatabaseVersionTheDbInfoShouldReturnTheRightVersion() {
    // Given & When
    DbInfo dbInfo = new DbInfo();
    dbInfo.setVersion(1);

    // Then
    Assertions.assertThat(dbInfo.getVersion()).isOne();
  }

  @Test
  void withoutADatabaseVersionTheDbInfoShouldReturnTheVersionZero() {
    // Given & When
    DbInfo dbInfo = new DbInfo();

    // Then
    Assertions.assertThat(dbInfo.getVersion()).isZero();
  }
}
