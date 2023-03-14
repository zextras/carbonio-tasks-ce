// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql.datafetchers;

import com.google.common.collect.ImmutableMap;
import com.zextras.carbonio.tasks.Constants.GraphQL.ServiceInfo;
import com.zextras.carbonio.tasks.Constants.Service;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;

/**
 * This class is a simple {@link DataFetcher} to expose service information such as:
 *
 * <ul>
 *   <li>{@link ServiceInfo#NAME}
 *   <li>{@link ServiceInfo#VERSION}
 *   <li>{@link ServiceInfo#FLAVOUR}
 * </ul>
 *
 * It does not call any external service (e.g.: database) so the fetching operation is not
 * encapsulated in a {@link java.util.concurrent.CompletableFuture}.
 */
public class ServiceInfoDataFetcher implements DataFetcher<DataFetcherResult<Map<String, Object>>> {

  @Override
  public DataFetcherResult<Map<String, Object>> get(DataFetchingEnvironment environment) {
    Map<String, Object> serviceInfo =
        ImmutableMap.<String, Object>builder()
            .put(ServiceInfo.NAME, Service.SERVICE_NAME)
            .put(ServiceInfo.VERSION, Service.VERSION)
            .put(ServiceInfo.FLAVOUR, Service.FLAVOUR)
            .build();

    return DataFetcherResult.<Map<String, Object>>newResult().data(serviceInfo).build();
  }
}
