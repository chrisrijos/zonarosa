/*
 * Copyright (C) 2019 ZonaRosa Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package io.zonarosa.service.api.push.exceptions;

public class RemoteAttestationResponseExpiredException extends NonSuccessfulResponseCodeException {
  public RemoteAttestationResponseExpiredException(String message) {
    super(409, message);
  }
}
