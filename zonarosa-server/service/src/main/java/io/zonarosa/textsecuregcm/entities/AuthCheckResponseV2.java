/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import jakarta.validation.constraints.NotNull;

public record AuthCheckResponseV2(@Schema(description = "A dictionary with the auth check results: `SVR Credentials -> 'match'/'no-match'/'invalid'`")
                                  @NotNull Map<String, Result> matches) {

  public enum Result {
    MATCH("match"),
    NO_MATCH("no-match"),
    INVALID("invalid");

    private final String clientCode;

    Result(final String clientCode) {
      this.clientCode = clientCode;
    }

    @JsonValue
    public String clientCode() {
      return clientCode;
    }
  }
}
