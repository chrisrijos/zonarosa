/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

public class DirectoryV2Configuration {

  private final DirectoryV2ClientConfiguration clientConfiguration;

  @JsonCreator
  public DirectoryV2Configuration(@JsonProperty("client") DirectoryV2ClientConfiguration clientConfiguration) {
    this.clientConfiguration = clientConfiguration;
  }

  @Valid
  public DirectoryV2ClientConfiguration getDirectoryV2ClientConfiguration() {
    return clientConfiguration;
  }
}
