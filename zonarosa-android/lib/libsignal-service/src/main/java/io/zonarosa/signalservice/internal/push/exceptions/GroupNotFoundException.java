package io.zonarosa.service.internal.push.exceptions;

import io.zonarosa.service.api.push.exceptions.NonSuccessfulResponseCodeException;

public final class GroupNotFoundException extends NonSuccessfulResponseCodeException {
  public GroupNotFoundException() {
    super(404);
  }
}
