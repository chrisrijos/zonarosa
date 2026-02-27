package io.zonarosa.service.internal.push.exceptions;

import io.zonarosa.service.api.push.exceptions.NonSuccessfulResponseCodeException;

import javax.annotation.Nonnull;

public final class GroupPatchNotAcceptedException extends NonSuccessfulResponseCodeException {
  public GroupPatchNotAcceptedException() {
    super(400);
  }

  public GroupPatchNotAcceptedException(@Nonnull String message) {
    super(400, message);
  }
}
