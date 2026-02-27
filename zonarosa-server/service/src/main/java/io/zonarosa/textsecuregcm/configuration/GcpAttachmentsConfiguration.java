/*
 * Copyright 2013 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import io.dropwizard.validation.ValidationMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import io.zonarosa.server.configuration.secrets.SecretString;

public record GcpAttachmentsConfiguration(@NotBlank String domain,
                                          @NotBlank String email,
                                          @Min(1) int maxSizeInBytes,
                                          String pathPrefix,
                                          @NotNull SecretString rsaSigningKey) {
  @SuppressWarnings("unused")
  @ValidationMethod(message = "pathPrefix must be empty or start with /")
  public boolean isPathPrefixValid() {
    return StringUtils.isEmpty(pathPrefix) || pathPrefix.startsWith("/");
  }
}
