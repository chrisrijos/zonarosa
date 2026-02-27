package io.zonarosa.service.internal.push.exceptions;

import io.zonarosa.service.api.push.exceptions.NonSuccessfulResponseCodeException;

public final class GroupExistsException extends NonSuccessfulResponseCodeException {
  public GroupExistsException() {
    super(409);
  }
}
