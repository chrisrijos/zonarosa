/**
 * Copyright (C) 2014-2016 ZonaRosa Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package io.zonarosa.service.api.push.exceptions;

import java.io.IOException;

public class PushNetworkException extends IOException {

  public PushNetworkException(Exception exception) {
    super(exception);
  }

  public PushNetworkException(String s) {
    super(s);
  }

}
