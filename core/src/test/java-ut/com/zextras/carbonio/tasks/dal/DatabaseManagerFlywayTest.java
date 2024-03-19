// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal;

import com.zextras.carbonio.tasks.config.providers.utils.Utils;
import com.zextras.carbonio.tasks.dal.impl.DatabaseManagerFlyway;
import org.assertj.core.api.Assertions;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.mockito.Mockito.*;

class DatabaseManagerFlywayTest {
    private DatabaseManagerFlyway databaseManager;
    private Flyway flywayMock;

    @BeforeEach
    public void setUp(){
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
        Statement mockStatement = Mockito.mock(Statement.class);
        ResultSet mockResultSet = Mockito.mock(ResultSet.class);

        String query = "select version, script " +
            "from flyway_schema_history " +
            "where success = true " +
            "order by installed_on desc " +
            "limit 1";

        try {
            Mockito.when(flywayMock.getConfiguration().getDataSource().getConnection().createStatement()).thenReturn(mockStatement);
            Mockito.when(mockStatement.executeQuery(query)).thenReturn(mockResultSet);
            Mockito.when(mockResultSet.next()).thenReturn(true);
            Mockito.when(mockResultSet.getString("version")).thenReturn("1");
        }catch (SQLException e){
            Assertions.fail("something returns sqlexception and should not");
        }

        // When
        String databaseVersion = databaseManager.getDatabaseVersion();

        // Then
        Assertions.assertThat(databaseVersion).isEqualTo("1");
    }

    @Test
    void givenADatabaseNotInitializedGetDatabaseVersionShouldReturnZero() {
        // Given
        Statement mockStatement = Mockito.mock(Statement.class);

        String query = "select version, script " +
            "from flyway_schema_history " +
            "where success = true " +
            "order by installed_on desc " +
            "limit 1";

        try {
            Mockito.when(flywayMock.getConfiguration().getDataSource().getConnection().createStatement()).thenReturn(mockStatement);
            Mockito.when(mockStatement.executeQuery(query)).thenThrow(SQLException.class);
        }catch (SQLException e){
            Assertions.fail("something returns sqlexception and should not");
        }

        // When
        String databaseVersion = databaseManager.getDatabaseVersion();

        // Then
        Assertions.assertThat(databaseVersion).isEqualTo("0");
    }

    @Test
    void givenADatabaseTheIsDatabaseLiveShouldReturnTrue() {
        // Given
      try {
        Mockito.when(flywayMock.getConfiguration().getDataSource().getConnection().isValid(1)).thenReturn(true);
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
            Mockito.when(flywayMock.getConfiguration().getDataSource().getConnection()).thenThrow(SQLException.class);
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
            Mockito.when(flywayMock.getConfiguration().getDataSource().getConnection().isValid(1)).thenReturn(false);
        } catch (SQLException e) {
            Assertions.fail("getConnection returns sqlexception and should not");
        }

        // When
        boolean databaseLive = databaseManager.isDatabaseLive();

        // Then
        Assertions.assertThat(databaseLive).isFalse();
    }

    @Test
    void givenADatabaseWithVersionEqualToLastLocalMigrationScriptIsDatabaseCorrectVersionShouldReturnTrue() {
        // Given
        Statement mockStatement = Mockito.mock(Statement.class);
        ResultSet mockResultSet = Mockito.mock(ResultSet.class);
        Location location = new Location("classpath://test/test");
        Location[] locations = new Location[]{location};
        MockedStatic<Utils> utilities = Mockito.mockStatic(Utils.class);
        utilities.when(() -> Utils.isScriptInPath("V1_FAKE_SCRIPT.sql", location.getPath())).thenReturn(true);

        String query = "select version, script " +
            "from flyway_schema_history " +
            "where success = true " +
            "order by installed_on desc " +
            "limit 1";

        try {
            Mockito.when(flywayMock.getConfiguration().getDataSource().getConnection().createStatement()).thenReturn(mockStatement);
            Mockito.when(mockStatement.executeQuery(query)).thenReturn(mockResultSet);
            Mockito.when(mockResultSet.next()).thenReturn(true);
            Mockito.when(mockResultSet.getString("script")).thenReturn("V1_FAKE_SCRIPT.sql");
            Mockito.when(flywayMock.getConfiguration().getLocations()).thenReturn(locations);
        }catch (SQLException e){
            Assertions.fail("something returns sqlexception and should not");
        }

        // When
        boolean databaseCorrectVersion = databaseManager.isDatabaseCorrectVersion();

        // Then
        Assertions.assertThat(databaseCorrectVersion).isTrue();

        utilities.close();
    }

    @Test
    void givenADatabaseWithVersionDifferentFromLastLocalMigrationScriptIsDatabaseCorrectVersionShouldReturnFalse() {
        // Given
        Statement mockStatement = Mockito.mock(Statement.class);
        ResultSet mockResultSet = Mockito.mock(ResultSet.class);
        Location location = new Location("classpath://test/test");
        Location[] locations = new Location[]{location};
        MockedStatic<Utils> utilities = Mockito.mockStatic(Utils.class);
        utilities.when(() -> Utils.isScriptInPath("V2_FAKE_SCRIPT.sql", location.getPath())).thenReturn(false);

        String query = "select version, script " +
            "from flyway_schema_history " +
            "where success = true " +
            "order by installed_on desc " +
            "limit 1";

        try {
            Mockito.when(flywayMock.getConfiguration().getDataSource().getConnection().createStatement()).thenReturn(mockStatement);
            Mockito.when(mockStatement.executeQuery(query)).thenReturn(mockResultSet);
            Mockito.when(mockResultSet.next()).thenReturn(true);
            Mockito.when(mockResultSet.getString("script")).thenReturn("V2_FAKE_SCRIPT.sql");
            Mockito.when(flywayMock.getConfiguration().getLocations()).thenReturn(locations);
        }catch (SQLException e){
            Assertions.fail("something returns sqlexception and should not");
        }

        // When
        boolean databaseCorrectVersion = databaseManager.isDatabaseCorrectVersion();

        // Then
        Assertions.assertThat(databaseCorrectVersion).isFalse();

        utilities.close();
    }
}
