package io.zonarosa.service.internal.push;

import com.fasterxml.jackson.annotation.JsonProperty;

class SetUsernameResponse {
  @JsonProperty
  private String username;

  SetUsernameResponse() {}

  String getUsername() {
    return username;
  }
}
