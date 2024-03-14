// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal.repositories.impl;

import com.zextras.carbonio.tasks.Constants.Database.Tables;
import com.zextras.carbonio.tasks.dal.DatabaseConnectionManager;
import com.zextras.carbonio.tasks.dal.dao.DbInfo;
import com.zextras.carbonio.tasks.dal.repositories.DbInfoRepository;
import io.ebean.Database;
import io.ebean.SqlRow;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DbInfoRepositoryEbeanTest {

  private Database ebeanDatabaseMock;
  private DbInfoRepository dbInfoRepository;

  @BeforeEach
  void setUp() {
    ebeanDatabaseMock = Mockito.mock(Database.class, Mockito.RETURNS_DEEP_STUBS);
    DatabaseConnectionManager connectionManagerMock = Mockito.mock(DatabaseConnectionManager.class);
    Mockito.when(connectionManagerMock.getEbeanDatabase()).thenReturn(ebeanDatabaseMock);
    dbInfoRepository = new DbInfoRepositoryEbean(connectionManagerMock);
  }

  @Test
  void givenAnInitializedDatabaseGetDatabaseVersionShouldReturnTheRightDbVersion() {
    // Given
    DbInfo dbInfoMock = Mockito.mock(DbInfo.class);
    Mockito.when(dbInfoMock.getVersion()).thenReturn(1);
    Mockito.when(ebeanDatabaseMock.find(DbInfo.class).findOneOrEmpty())
        .thenReturn(Optional.of(dbInfoMock));

    // When
    int databaseVersion = dbInfoRepository.getDatabaseVersion();

    // Then
    Assertions.assertThat(databaseVersion).isOne();
  }

  @Test
  void givenADatabaseNotInitializedGetDatabaseVersionShouldReturnZero() {
    // Given
    Mockito.when(ebeanDatabaseMock.find(DbInfo.class).findOneOrEmpty())
        .thenReturn(Optional.empty());

    // When
    int databaseVersion = dbInfoRepository.getDatabaseVersion();

    // Then
    Assertions.assertThat(databaseVersion).isZero();
  }

  @Test
  void givenAnInitializedDatabaseTheIsDatabaseLiveShouldReturnTrue() {
    // Given
    Mockito.when(ebeanDatabaseMock.find(DbInfo.class).findOneOrEmpty())
        .thenReturn(Optional.of(Mockito.mock(DbInfo.class)));

    // When
    boolean isDatabaseLive = dbInfoRepository.isDatabaseLive();

    // Then
    Assertions.assertThat(isDatabaseLive).isTrue();
  }

  @Test
  void givenADatabaseNotInitializedTheIsDatabaseLiveShouldReturnFalse() {
    // Given
    Mockito.when(ebeanDatabaseMock.find(DbInfo.class).findOneOrEmpty())
        .thenReturn(Optional.empty());

    // When
    boolean isDatabaseLive = dbInfoRepository.isDatabaseLive();

    // Then
    Assertions.assertThat(isDatabaseLive).isFalse();
  }
}
