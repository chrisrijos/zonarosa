/**
 * Copyright (C) 2014-2016 ZonaRosa Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */
package io.zonarosa.service.api.push.exceptions;

public class ExpectationFailedException extends NonSuccessfulResponseCodeException {
  public ExpectationFailedException() {
    super(417);
  }
}
