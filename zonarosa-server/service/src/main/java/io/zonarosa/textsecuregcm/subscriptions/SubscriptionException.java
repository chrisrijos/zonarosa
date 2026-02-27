/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.subscriptions;

import java.util.Optional;
import javax.annotation.Nullable;

public class SubscriptionException extends Exception {

  private @Nullable String errorDetail;

  public SubscriptionException(Exception cause) {
    this(cause, null);
  }

  SubscriptionException(Exception cause, String errorDetail) {
    super(cause);
    this.errorDetail = errorDetail;
  }

  /**
   * @return An error message suitable to include in a client response
   */
  public Optional<String> errorDetail() {
    return Optional.ofNullable(errorDetail);
  }

}
