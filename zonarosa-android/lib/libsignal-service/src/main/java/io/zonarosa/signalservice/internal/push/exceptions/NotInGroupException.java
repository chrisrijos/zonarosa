package io.zonarosa.service.internal.push.exceptions;

import io.zonarosa.service.api.push.exceptions.NonSuccessfulResponseCodeException;

public final class NotInGroupException extends NonSuccessfulResponseCodeException {
  public NotInGroupException() {
    super(403);
  }
}
