// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestUtils {

  public static String queryPayload(String query) {
    return String.format("{\"query\":\"%s\"}", query);
  }

  public static String mutationPayload(String mutation) {
    return String.format("{\"mutation\":\"%s\"}", mutation);
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> jsonResponseToMap(String json, String operation) {
    try {
      Map<String, Object> result = new ObjectMapper().readValue(json, HashMap.class);

      if (result.get("data") != null) {
        Map<String, Object> data = (Map<String, Object>) result.get("data");

        if (data.get(operation) != null) {
          return (Map<String, Object>) data.get(operation);
        }
      }
      return Collections.emptyMap();

    } catch (JsonProcessingException exception) {
      return Collections.emptyMap();
    }
  }

  @SuppressWarnings("unchecked")
  public static List<Map<String, Object>> jsonResponseToList(String json, String operation) {
    try {
      Map<String, Object> result = new ObjectMapper().readValue(json, Map.class);

      if (result.get("data") != null) {
        Map<String, Object> data = (Map<String, Object>) result.get("data");

        if (data.get(operation) != null) {
          return (List<Map<String, Object>>) data.get(operation);
        }
      }
      return Collections.emptyList();

    } catch (JsonProcessingException exception) {
      return Collections.emptyList();
    }
  }

  public static List<String> jsonResponseToErrors(String json) {
    try {
      Map<String, Object> result = new ObjectMapper().readValue(json, Map.class);

      if (result.get("errors") != null) {
        List<Map<String, Object>> errors = (List<Map<String, Object>>) result.get("errors");

        return errors.stream()
            .map(error -> (String) error.get("message"))
            .collect(Collectors.toList());
      }
    } catch (JsonProcessingException exception) {
      return Collections.emptyList();
    }
    return Collections.emptyList();
  }
}
