/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.backup;

import java.io.IOException;

public class SourceObjectNotFoundException extends IOException {
  public SourceObjectNotFoundException() {
    super();
  }
  public SourceObjectNotFoundException(String message) {
    super(message);
  }
}
