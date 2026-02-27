/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.messages;

class StorageFailedException extends Exception {
  private final String sender;
  private final int    senderDevice;

  StorageFailedException(Exception e, String sender, int senderDevice) {
    super(e);
    this.sender       = sender;
    this.senderDevice = senderDevice;
  }

  public String getSender() {
    return sender;
  }

  public int getSenderDevice() {
    return senderDevice;
  }
}
