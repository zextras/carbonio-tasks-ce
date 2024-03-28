// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal;

import static org.mockito.Mockito.*;

import com.zextras.carbonio.tasks.dal.impl.DatabaseManagerFlyway;
import java.sql.SQLException;
import org.assertj.core.api.Assertions;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DatabaseManagerFlywayTest {
  private DatabaseManagerFlyway databaseManager;
  private Flyway flywayMock;

  @BeforeEach
  public void setUp() {
    flywayMock = Mockito.mock(Flyway.class, RETURNS_DEEP_STUBS);
    databaseManager = new DatabaseManagerFlyway(flywayMock);
  }

  @Test
  void givenAConnectionAndADbNTheDbShouldBeMigrated() {
    // Given
    when(flywayMock.migrate()).thenReturn(mock(MigrateResult.class));

    // When
    databaseManager.initialize();

    // Then
    Mockito.verify(flywayMock, Mockito.times(1)).migrate();
  }

  @Test
  void givenAnInitializedDatabaseGetDatabaseVersionShouldReturnTheRightDbVersion() {
    // Given
    when(flywayMock.info().current().getVersion().getVersion()).thenReturn("1");

    // When
    String databaseVersion = databaseManager.getDatabaseVersion();

    // Then
    Assertions.assertThat(databaseVersion).isEqualTo("1");
  }

  @Test
  void givenADatabaseNotInitializedGetDatabaseVersionShouldReturnZero() {
    // Given
    when(flywayMock.info().current()).thenReturn(null);

    // When
    String databaseVersion = databaseManager.getDatabaseVersion();

    // Then
    Assertions.assertThat(databaseVersion).isEqualTo("0");
  }

  @Test
  void givenADatabaseTheIsDatabaseLiveShouldReturnTrue() {
    // Given
    try {
      Mockito.when(flywayMock.getConfiguration().getDataSource().getConnection().isValid(1))
          .thenReturn(true);
    } catch (SQLException e) {
      Assertions.fail("getConnection returns sqlexception and should not");
    }

    // When
    boolean databaseLive = databaseManager.isDatabaseLive();

    // Then
    Assertions.assertThat(databaseLive).isTrue();
  }

  @Test
  void givenANotReachableDatabaseTheIsDatabaseLiveShouldReturnFalse() {
    // Given
    try {
      Mockito.when(flywayMock.getConfiguration().getDataSource().getConnection())
          .thenThrow(SQLException.class);
    } catch (SQLException e) {
      Assertions.fail("getConnection returns sqlexception and should not");
    }

    // When
    boolean databaseLive = databaseManager.isDatabaseLive();

    // Then
    Assertions.assertThat(databaseLive).isFalse();
  }

  @Test
  void givenANotRespondingInTimeDatabaseTheIsDatabaseLiveShouldReturnFalse() {
    // Given
    try {
      Mockito.when(flywayMock.getConfiguration().getDataSource().getConnection().isValid(1))
          .thenReturn(false);
    } catch (SQLException e) {
      Assertions.fail("getConnection returns sqlexception and should not");
    }

    // When
    boolean databaseLive = databaseManager.isDatabaseLive();

    // Then
    Assertions.assertThat(databaseLive).isFalse();
  }

  @Test
  void
      givenADatabaseWithVersionEqualToLastLocalMigrationScriptIsDatabaseCorrectVersionShouldReturnTrue() {
    // Given
    when(flywayMock.info().current().getPhysicalLocation())
        .thenReturn("/db/migration/V1__FAKE.sql");

    // When
    boolean databaseCorrectVersion = databaseManager.isDatabaseCorrectVersion();

    // Then
    Assertions.assertThat(databaseCorrectVersion).isTrue();
  }

  @Test
  void
      givenADatabaseWithVersionDifferentFromLastLocalMigrationScriptIsDatabaseCorrectVersionShouldReturnFalse() {
    // Given
    when(flywayMock.info().current().getPhysicalLocation()).thenReturn("");

    // When
    boolean databaseCorrectVersion = databaseManager.isDatabaseCorrectVersion();

    // Then
    Assertions.assertThat(databaseCorrectVersion).isFalse();
  }
}
