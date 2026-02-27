/**
 * Copyright (C) 2014-2016 ZonaRosa Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package io.zonarosa.service.internal.push;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class StaleDevices {

  @JsonProperty
  private List<Integer> staleDevices;

  public List<Integer> getStaleDevices() {
    return staleDevices;
  }
}
