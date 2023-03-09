// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal;

import com.zextras.carbonio.tasks.Simulator;
import com.zextras.carbonio.tasks.Simulator.SimulatorBuilder;
import io.ebean.Database;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;

public class DatabaseConnectionManagerIT {

  @Test
  public void givenAReachableDatabaseTheGetEbeanDatabaseShouldReturnAnEbeanDatabase() {
    // Given
    Simulator simulator =
        SimulatorBuilder.aSimulator().init().withDatabase().withServiceDiscover().build().start();

    DatabaseConnectionManager connectionManager =
        simulator.getInjector().getInstance(DatabaseConnectionManager.class);

    // When
    Database database = connectionManager.getEbeanDatabase();

    // Then
    Assertions.assertThat(database.sqlQuery("SELECT 1").findOneOrEmpty()).isPresent();

    simulator.stopAll();
  }

  @Test
  public void givenAnUnreachableDatabaseTheGetEbeanDatabaseShouldThrownAnException() {
    // Given
    Simulator simulator = SimulatorBuilder.aSimulator().init().build().start();

    DatabaseConnectionManager connectionManager =
        simulator.getInjector().getInstance(DatabaseConnectionManager.class);

    // When
    ThrowableAssert.ThrowingCallable throwable = connectionManager::getEbeanDatabase;

    // Then
    Assertions.assertThatRuntimeException().isThrownBy(throwable);

    simulator.stopAll();
  }

  @Test
  public void givenAReachableDatabaseWithAWrongNameTheGetEbeanDatabaseShouldThrownAnException() {
    // Given
    Simulator simulator = SimulatorBuilder.aSimulator().init().withDatabase().build().start();

    DatabaseConnectionManager connectionManager =
        simulator.getInjector().getInstance(DatabaseConnectionManager.class);

    // When
    ThrowableAssert.ThrowingCallable throwable = connectionManager::getEbeanDatabase;

    // Then
    Assertions.assertThatRuntimeException().isThrownBy(throwable);

    simulator.stopAll();
  }
}
