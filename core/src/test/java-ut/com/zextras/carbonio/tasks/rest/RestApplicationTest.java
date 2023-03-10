// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.rest;

import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class RestApplicationTest {

  @Test
  public void theRestApplicationShouldReturnTheControllerAPIClasses() {
    // Given & When
    Set<Class<?>> classes = new RestApplication().getClasses();

    // Then
    // Note: testing which instance of classes this set contains is almost impossible.
    // I am testing only the number of classes
    Assertions.assertThat(classes.size()).isEqualTo(1);
  }
}
