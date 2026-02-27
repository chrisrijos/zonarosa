/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.sms;

import androidx.annotation.NonNull;

public final class ReceivedSmsEvent {

  public static final int CODE_LENGTH = 6;

  private final @NonNull String code;

  public ReceivedSmsEvent(@NonNull String code) {
    this.code = code;
  }

  public @NonNull String getCode() {
    return code;
  }
}
