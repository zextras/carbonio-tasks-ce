// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.utils.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceDiscoverHttpClient {

  private static final Logger logger = LoggerFactory.getLogger(ServiceDiscoverHttpClient.class);

  private final String serviceDiscoverURL;

  ServiceDiscoverHttpClient(String serviceDiscoverURL) {
    this.serviceDiscoverURL = serviceDiscoverURL;
  }

  public static ServiceDiscoverHttpClient atURL(String url, String serviceName) {
    String completeUrl = String.format("%s/v1/kv/%s/", url, serviceName);
    return new ServiceDiscoverHttpClient(completeUrl);
  }

  public static ServiceDiscoverHttpClient defaultURL(String serviceName) {
    String defaultUrl = String.format("http://localhost:8500/v1/kv/%s/", serviceName);
    return new ServiceDiscoverHttpClient(defaultUrl);
  }

  public Optional<String> getConfig(String configKey) {
    try (CloseableHttpClient httpClient = HttpClients.createMinimal()) {
      HttpGet request = new HttpGet(serviceDiscoverURL + configKey);
      request.setHeader("X-Consul-Token", System.getenv("CONSUL_HTTP_TOKEN"));

      // With the response handler the 2xx status code check is already done internally
      BasicHttpClientResponseHandler responseHandler = new BasicHttpClientResponseHandler();
      String bodyResponse = httpClient.execute(request, responseHandler);
      String value = new ObjectMapper().readTree(bodyResponse).get(0).get("Value").asText();
      String valueDecoded = new String(Base64.decodeBase64(value), StandardCharsets.UTF_8).trim();

      return Optional.of(valueDecoded);

    } catch (HttpResponseException exception) {
      logger.warn(String.format("Unable to retrieve the config %s from consul", configKey));
      return Optional.empty();

    } catch (IOException exception) {
      logger.warn(
          String.format(
              "Unable to decode the ServiceDiscover response when '%s' key config is retrieved. %s",
              configKey, exception));
      return Optional.empty();
    }
  }
}
