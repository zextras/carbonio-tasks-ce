// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal;

import com.zextras.carbonio.tasks.dal.repositories.DbInfoRepository;
import io.ebean.Database;
import java.net.URL;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class DatabaseInitializerTest {

  @Test
  public void givenAConnectionAndADbNotInitializedTheDbShouldBeInitializedWithTheRightScript() {
    // Given
    DbInfoRepository dbInfoRepositoryMock = Mockito.mock(DbInfoRepository.class);
    Mockito.when(dbInfoRepositoryMock.isDatabaseInitialized()).thenReturn(false);

    Database databaseMock = Mockito.mock(Database.class, Mockito.RETURNS_DEEP_STUBS);
    DatabaseConnectionManager databaseConnectionManagerMock =
        Mockito.mock(DatabaseConnectionManager.class);
    Mockito.when(databaseConnectionManagerMock.getEbeanDatabase()).thenReturn(databaseMock);

    DatabaseInitializer databaseInitializer =
        new DatabaseInitializer(databaseConnectionManagerMock, dbInfoRepositoryMock);

    // When
    databaseInitializer.initialize();

    // Then
    Mockito.verify(dbInfoRepositoryMock, Mockito.times(1)).isDatabaseInitialized();
    Mockito.verify(databaseConnectionManagerMock, Mockito.times(1)).getEbeanDatabase();

    ArgumentCaptor<URL> urlCaptor = ArgumentCaptor.forClass(URL.class);
    Mockito.verify(databaseMock.script(), Mockito.times(1)).run(urlCaptor.capture());
    Assertions.assertThat(urlCaptor.getValue().getPath()).contains("sql/postgresql_1.sql");
  }

  @Test
  public void givenAConnectionAndADbInitializedTheDbShouldBeNothing() {
    // Given
    DbInfoRepository dbInfoRepositoryMock = Mockito.mock(DbInfoRepository.class);
    Mockito.when(dbInfoRepositoryMock.isDatabaseInitialized()).thenReturn(true);

    DatabaseConnectionManager databaseConnectionManagerMock =
        Mockito.mock(DatabaseConnectionManager.class);

    DatabaseInitializer databaseInitializer =
        new DatabaseInitializer(databaseConnectionManagerMock, dbInfoRepositoryMock);

    // When
    databaseInitializer.initialize();

    // Then
    Mockito.verify(dbInfoRepositoryMock, Mockito.times(1)).isDatabaseInitialized();
    Mockito.verify(databaseConnectionManagerMock, Mockito.times(0)).getEbeanDatabase();
  }
}
