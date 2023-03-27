// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.rest.types.health;

import java.util.Collections;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class HealthStatusTest {

  @Test
  public void givenAllHealthStatusAttributesTheSettersShouldInitializeThemCorrectly() {
    // Given & When
    ServiceHealth serviceHealth = new ServiceHealth();
    serviceHealth.setName("dependency").setType(DependencyType.OPTIONAL);

    HealthStatus healthStatus =
        new HealthStatus().setReady(true).setDependencies(Collections.singletonList(serviceHealth));

    // Then
    Assertions.assertThat(healthStatus.isReady()).isTrue();
    Assertions.assertThat(healthStatus.getDependencies().size()).isEqualTo(1);
    Assertions.assertThat(healthStatus.getDependencies().get(0).getName()).isEqualTo("dependency");
  }
}
