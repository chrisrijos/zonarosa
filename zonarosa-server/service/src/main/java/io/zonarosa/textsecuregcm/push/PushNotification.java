/*
 * Copyright 2013-2022 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.push;

import io.zonarosa.server.storage.Account;
import io.zonarosa.server.storage.Device;
import javax.annotation.Nullable;

public record PushNotification(String deviceToken,
                               TokenType tokenType,
                               NotificationType notificationType,
                               @Nullable String data,
                               @Nullable Account destination,
                               @Nullable Device destinationDevice,
                               boolean urgent) {

  public enum NotificationType {
    NOTIFICATION,
    ATTEMPT_LOGIN_NOTIFICATION_HIGH_PRIORITY,
    CHALLENGE,
    RATE_LIMIT_CHALLENGE
  }

  public enum TokenType {
    FCM,
    APN
  }
}
