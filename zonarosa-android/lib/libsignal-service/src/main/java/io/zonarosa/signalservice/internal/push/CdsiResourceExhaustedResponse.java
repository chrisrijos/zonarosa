package io.zonarosa.service.internal.push;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response for {@link io.zonarosa.service.api.push.exceptions.CdsiResourceExhaustedException}
 */
public class CdsiResourceExhaustedResponse {
  @JsonProperty("retry_after")
  private int retryAfter;

  public int getRetryAfter() {
    return retryAfter;
  }
}
