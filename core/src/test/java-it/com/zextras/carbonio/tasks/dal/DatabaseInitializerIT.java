// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal;

import com.google.inject.Injector;
import com.zextras.carbonio.tasks.Simulator;
import com.zextras.carbonio.tasks.Simulator.SimulatorBuilder;
import com.zextras.carbonio.tasks.dal.dao.DbInfo;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class DatabaseInitializerIT {

  @Test
  public void givenAReachableDatabaseTheDatabaseShouldBeCorrectlyInitialized() {
    // Given
    Simulator simulator =
        SimulatorBuilder.aSimulator().init().withDatabase().withServiceDiscover().build().start();

    Injector injector = simulator.getInjector();

    DatabaseInitializer databaseInitializer = injector.getInstance(DatabaseInitializer.class);
    DatabaseConnectionManager databaseConnectionManager =
        injector.getInstance(DatabaseConnectionManager.class);

    // When
    databaseInitializer.initialize();

    // Then
    Optional<DbInfo> optDbInfo =
        databaseConnectionManager.getEbeanDatabase().find(DbInfo.class).findOneOrEmpty();

    Assertions.assertThat(optDbInfo).isPresent();
    Assertions.assertThat(optDbInfo.get().getVersion()).isEqualTo(1);

    simulator.stopAll();
  }
}
