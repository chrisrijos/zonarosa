/*
 * Copyright 2013-2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.storage;

public class ChunkProcessingFailedException extends Exception {

  public ChunkProcessingFailedException(String message) {
    super(message);
  }

  public ChunkProcessingFailedException(Exception cause) {
    super(cause);
  }
}
