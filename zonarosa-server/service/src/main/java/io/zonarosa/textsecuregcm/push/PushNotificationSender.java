/*
 * Copyright 2013-2022 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.push;

import java.util.concurrent.CompletableFuture;

public interface PushNotificationSender {

  CompletableFuture<SendPushNotificationResult> sendNotification(PushNotification notification);
}
