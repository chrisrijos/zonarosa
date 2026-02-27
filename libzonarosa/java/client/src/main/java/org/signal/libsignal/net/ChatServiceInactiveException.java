//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

/** Indicates that an operation on the {@code ChatService} has been called before */
public class ChatServiceInactiveException extends ChatServiceException {
  public ChatServiceInactiveException(String message) {
    super(message);
  }
}
