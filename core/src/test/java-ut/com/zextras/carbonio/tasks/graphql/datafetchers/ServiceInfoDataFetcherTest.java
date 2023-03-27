// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql.datafetchers;

import com.zextras.carbonio.tasks.Constants.Service;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ServiceInfoDataFetcherTest {

  @Test
  void givenAServiceInfoRequestTheServiceInfoDataFetcherShouldReturnTheServiceInfo() {
    // Given
    ServiceInfoDataFetcher serviceInfoDataFetcher = new ServiceInfoDataFetcher();
    DataFetchingEnvironment environmentMock = Mockito.mock(DataFetchingEnvironment.class);

    // When
    DataFetcherResult<Map<String, Object>> dataFetcherResult =
        serviceInfoDataFetcher.get(environmentMock);

    // Then
    // Generally I don't like to use constants during the assertions but the last two change based
    // on the current version of the service and the flavour of the project.
    Assertions.assertThat(dataFetcherResult.getErrors()).isEmpty();
    Assertions.assertThat(dataFetcherResult.getData())
        .hasSize(3)
        .containsEntry("name", "carbonio-tasks")
        .containsEntry("version", Service.VERSION)
        .containsEntry("flavour", Service.FLAVOUR);
  }
}
