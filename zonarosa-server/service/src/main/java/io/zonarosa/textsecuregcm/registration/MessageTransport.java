/*
 * Copyright 2022 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.registration;

/**
 * A message transport is a medium via which verification codes can be delivered to a destination phone.
 */
public enum MessageTransport {
  SMS,
  VOICE
}
