/*
 * Copyright 2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration.dynamic;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;

public class DynamicPaymentsConfiguration {

  @JsonProperty
  private List<String> disallowedPrefixes = Collections.emptyList();

  public List<String> getDisallowedPrefixes() {
    return disallowedPrefixes;
  }
}
