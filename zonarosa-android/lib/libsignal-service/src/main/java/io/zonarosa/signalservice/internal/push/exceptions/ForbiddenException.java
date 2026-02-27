package io.zonarosa.service.internal.push.exceptions;


import io.zonarosa.service.api.push.exceptions.NonSuccessfulResponseCodeException;

import java.util.Optional;

public final class ForbiddenException extends NonSuccessfulResponseCodeException {
  private Optional<String> reason;

  public ForbiddenException() {
    this(Optional.empty());
  }

  public ForbiddenException(Optional<String> reason) {
    super(403);
    this.reason = reason;
  }

  public Optional<String> getReason() {
    return reason;
  }
}
