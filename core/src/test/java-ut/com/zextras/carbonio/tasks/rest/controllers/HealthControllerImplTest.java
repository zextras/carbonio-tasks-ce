// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.rest.controllers;

import javax.ws.rs.core.Response;
import org.assertj.core.api.Assertions;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class HealthControllerImplTest {

  @Test
  public void healthLiveShouldReturn204StatusCodeResponse() {
    // Given & When
    try (Response response = new HealthControllerImpl().healthLive()) {

      // Then
      Assertions.assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);
      Assertions.assertThat(response.getEntity()).isNull();
    }
  }

  @Test
  public void givenAReadyDatabaseTheHealthReadyShouldReturn204StatusCodeResponse() {
    // Given & When
    try (Response response = new HealthControllerImpl().healthReady()) {

      // Then
      Assertions.assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);
      Assertions.assertThat(response.getEntity()).isNull();
    }
  }

  @Disabled
  @Test
  public void givenAUnreachableDatabaseTheHealthReadyShouldReturn502StatusCodeResponse() {
    // Given & When
    try (Response response = new HealthControllerImpl().healthReady()) {

      // Then
      Assertions.assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY_502);
      Assertions.assertThat(response.getEntity()).isNull();
    }
  }
}
