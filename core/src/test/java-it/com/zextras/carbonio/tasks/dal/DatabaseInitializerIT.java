// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.dal;

import com.google.inject.Injector;
import com.zextras.carbonio.tasks.Constants.Config.Database;
import com.zextras.carbonio.tasks.Constants.Config.Properties;
import com.zextras.carbonio.tasks.Simulator;
import com.zextras.carbonio.tasks.Simulator.SimulatorBuilder;
import com.zextras.carbonio.tasks.dal.dao.DbInfo;
import com.zextras.carbonio.tasks.dal.dao.Priority;
import com.zextras.carbonio.tasks.dal.dao.Status;
import com.zextras.carbonio.tasks.dal.dao.Task;
import com.zextras.carbonio.tasks.dal.repositories.TaskRepository;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

public class DatabaseInitializerIT {

  private PostgreSQLContainer<?> postgreSQLContainer;
  private Simulator simulator;

  /**
   * The setup does <b>>not</b> use the database created and initialized in the simulator ({@link
   * SimulatorBuilder#withDatabase()}) because it needs to have an empty database to test the
   * initialization process.
   */
  @BeforeEach
  public void setUp() {
    postgreSQLContainer = new PostgreSQLContainer<>("postgres:12.14");
    postgreSQLContainer
        .withDatabaseName(Database.NAME)
        .withUsername(Database.USERNAME)
        .withPassword(Simulator.DATABASE_PASSWORD)
        .start();

    // Set the System.properties for the datasource created in TaskConfig
    System.setProperty(Properties.DATABASE_URL, postgreSQLContainer.getHost());
    System.setProperty(
        Properties.DATABASE_PORT, String.valueOf(postgreSQLContainer.getFirstMappedPort()));

    simulator = SimulatorBuilder.aSimulator().init().withServiceDiscover().build().start();
  }

  @AfterEach
  public void cleanUp() {
    simulator.stopAll();
    postgreSQLContainer.stop();
  }

  @Test
  public void givenAReachableDatabaseTheDatabaseShouldBeCorrectlyInitialized() {
    // Given
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
  }

  @Test
  public void givenAReachableAndInitializedDatabaseTheInitializerShouldNotReInitializeTheDb() {
    // Given
    Injector injector = simulator.getInjector();

    DatabaseInitializer databaseInitializer = injector.getInstance(DatabaseInitializer.class);
    DatabaseConnectionManager dbConnection = injector.getInstance(DatabaseConnectionManager.class);

    // First initialization
    databaseInitializer.initialize();

    // Insert a new task
    injector
        .getInstance(TaskRepository.class)
        .createTask("user-id", "title", null, Priority.HIGH, Status.OPEN, null, null);

    // When
    // Second initialization
    databaseInitializer.initialize();

    // Then
    Optional<DbInfo> optDbInfo =
        dbConnection.getEbeanDatabase().find(DbInfo.class).findOneOrEmpty();

    Assertions.assertThat(optDbInfo).isPresent();
    Assertions.assertThat(optDbInfo.get().getVersion()).isEqualTo(1);

    // Checking if the content of the database is still there. If yes, it means that the second
    // initialization was correctly skipped
    List<Task> tasks = dbConnection.getEbeanDatabase().find(Task.class).findList();
    Assertions.assertThat(tasks.size()).isEqualTo(1);
  }
}
