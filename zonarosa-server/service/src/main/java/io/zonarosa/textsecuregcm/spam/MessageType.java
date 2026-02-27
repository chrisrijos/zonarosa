/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.spam;

public enum MessageType {
  INDIVIDUAL_IDENTIFIED_SENDER,
  SYNC,
  INDIVIDUAL_SEALED_SENDER,
  MULTI_RECIPIENT_SEALED_SENDER,
  INDIVIDUAL_STORY,
  MULTI_RECIPIENT_STORY,
}
