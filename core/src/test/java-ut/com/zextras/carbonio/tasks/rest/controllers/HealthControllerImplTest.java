// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.rest.controllers;

import com.zextras.carbonio.tasks.rest.services.HealthService;
import com.zextras.carbonio.tasks.rest.types.health.DependencyType;
import com.zextras.carbonio.tasks.rest.types.health.HealthStatus;
import com.zextras.carbonio.tasks.rest.types.health.ServiceHealth;
import java.util.Collections;
import javax.ws.rs.core.Response;
import org.assertj.core.api.Assertions;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class HealthControllerImplTest {

  private HealthService healthServiceMock;

  @BeforeEach
  void setUp() {
    healthServiceMock = Mockito.mock(HealthService.class);
  }

  @Test
  void
      givenAllDependenciesReadyTheHealthShouldReturn200StatusCodeWithTheHealthStatusOfEachDependency() {
    // Given & When
    ServiceHealth dependencyHealth =
        new ServiceHealth()
            .setName("dependency-1")
            .setReady(true)
            .setLive(true)
            .setType(DependencyType.REQUIRED);

    HealthStatus healthStatus =
        new HealthStatus()
            .setReady(true)
            .setDependencies(Collections.singletonList(dependencyHealth));

    Mockito.when(healthServiceMock.getServiceHealthStatus()).thenReturn(healthStatus);

    try (Response response = new HealthControllerImpl(healthServiceMock).health()) {

      // Then
      Assertions.assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
      Assertions.assertThat(response.getEntity()).isNotNull();
      Mockito.verify(healthServiceMock, Mockito.times(1)).getServiceHealthStatus();
    }
  }

  @Test
  void givenADependencyNotReadyTheHealthShouldReturn502StatusCodeResponse() {
    // Given & When
    HealthStatus healthStatus = new HealthStatus().setReady(false);

    Mockito.when(healthServiceMock.getServiceHealthStatus()).thenReturn(healthStatus);
    try (Response response = new HealthControllerImpl(healthServiceMock).health()) {

      // Then
      Assertions.assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY_502);
      Assertions.assertThat(response.getEntity()).isNotNull();
      Mockito.verify(healthServiceMock, Mockito.times(1)).getServiceHealthStatus();
    }
  }

  @Test
  void healthLiveShouldReturn204StatusCodeResponse() {
    // Given & When
    try (Response response = new HealthControllerImpl(healthServiceMock).healthLive()) {

      // Then
      Assertions.assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);
      Assertions.assertThat(response.getEntity()).isNull();
      Mockito.verifyNoInteractions(healthServiceMock);
    }
  }

  @Test
  void givenAllDependenciesReadyTheHealthReadyShouldReturn204StatusCodeResponse() {
    // Given & When
    Mockito.when(healthServiceMock.areServiceDependenciesReady()).thenReturn(true);
    try (Response response = new HealthControllerImpl(healthServiceMock).healthReady()) {

      // Then
      Assertions.assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);
      Assertions.assertThat(response.getEntity()).isNull();
      Mockito.verify(healthServiceMock, Mockito.times(1)).areServiceDependenciesReady();
    }
  }

  @Test
  void givenADependencyNotReadyTheHealthReadyShouldReturn502StatusCodeResponse() {
    // Given & When
    Mockito.when(healthServiceMock.areServiceDependenciesReady()).thenReturn(false);
    try (Response response = new HealthControllerImpl(healthServiceMock).healthReady()) {

      // Then
      Assertions.assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY_502);
      Assertions.assertThat(response.getEntity()).isNull();
      Mockito.verify(healthServiceMock, Mockito.times(1)).areServiceDependenciesReady();
    }
  }
}
