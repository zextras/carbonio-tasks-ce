// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.rest.types.health;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ServiceHealthTest {

  @Test
  void givenAllServiceHealthAttributesTheSettersShouldInitializeThemCorrectly() {
    // Given & When
    ServiceHealth serviceHealth = new ServiceHealth();
    serviceHealth
        .setName("dependency")
        .setLive(true)
        .setReady(false)
        .setType(DependencyType.REQUIRED);

    // Then
    Assertions.assertThat(serviceHealth.getName()).isEqualTo("dependency");
    Assertions.assertThat(serviceHealth.isLive()).isTrue();
    Assertions.assertThat(serviceHealth.isReady()).isFalse();
    Assertions.assertThat(serviceHealth.getType()).isEqualTo(DependencyType.REQUIRED);
  }
}
