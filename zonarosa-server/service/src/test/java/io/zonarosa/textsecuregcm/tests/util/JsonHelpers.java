/*
 * Copyright 2013 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.tests.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import io.dropwizard.util.Resources;
import io.zonarosa.server.util.SystemMapper;

public class JsonHelpers {

  private static final ObjectMapper objectMapper = SystemMapper.jsonMapper();

  public static String asJson(Object object) throws JsonProcessingException {
    return objectMapper.writeValueAsString(object);
  }

  public static <T> T fromJson(String value, Class<T> clazz) throws IOException {
    return objectMapper.readValue(value, clazz);
  }

  public static String jsonFixture(String filename) throws IOException {
    return objectMapper.writeValueAsString(
        objectMapper.readValue(Resources.getResource(filename), JsonNode.class));
  }
}
