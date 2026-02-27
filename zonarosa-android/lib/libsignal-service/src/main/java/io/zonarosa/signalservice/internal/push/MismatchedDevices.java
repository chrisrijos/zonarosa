/**
 * Copyright (C) 2014-2016 ZonaRosa Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package io.zonarosa.service.internal.push;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class MismatchedDevices {
  @JsonProperty
  public List<Integer> missingDevices;

  @JsonProperty
  public List<Integer> extraDevices;

  public List<Integer> getMissingDevices() {
    return missingDevices;
  }

  public List<Integer> getExtraDevices() {
    return extraDevices;
  }
}
