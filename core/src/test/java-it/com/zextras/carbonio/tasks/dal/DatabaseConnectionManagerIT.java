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

class DatabaseConnectionManagerIT {

  @Test
  void givenAReachableDatabaseTheGetEbeanDatabaseShouldReturnAnEbeanDatabase() {
    try (Simulator simulator =
        SimulatorBuilder.aSimulator().init().withDatabase().withServiceDiscover().build().start()) {

      // Given
      DatabaseConnectionManager connectionManager =
          simulator.getInjector().getInstance(DatabaseConnectionManager.class);

      // When
      Database database = connectionManager.getEbeanDatabase();

      // Then
      Assertions.assertThat(database.sqlQuery("SELECT 1").findOneOrEmpty()).isPresent();

      simulator.stopAll();
    }
  }

  @Test
  void givenAnUnreachableDatabaseTheGetEbeanDatabaseShouldThrownAnException() {
    // Given
    try (Simulator simulator = SimulatorBuilder.aSimulator().init().build().start()) {

      DatabaseConnectionManager connectionManager =
          simulator.getInjector().getInstance(DatabaseConnectionManager.class);

      // When
      ThrowableAssert.ThrowingCallable throwable = connectionManager::getEbeanDatabase;

      // Then
      Assertions.assertThatRuntimeException().isThrownBy(throwable);

      simulator.stopAll();
    }
  }

  @Test
  void givenAReachableDatabaseWithAWrongNameTheGetEbeanDatabaseShouldThrownAnException() {
    // Given
    try (Simulator simulator =
        SimulatorBuilder.aSimulator().init().withDatabase().build().start()) {

      DatabaseConnectionManager connectionManager =
          simulator.getInjector().getInstance(DatabaseConnectionManager.class);

      // When
      ThrowableAssert.ThrowingCallable throwable = connectionManager::getEbeanDatabase;

      // Then
      Assertions.assertThatRuntimeException().isThrownBy(throwable);

      simulator.stopAll();
    }
  }
}
