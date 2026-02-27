package io.zonarosa.service.api.push.exceptions;

public class UsernameTakenException extends NonSuccessfulResponseCodeException {
  public UsernameTakenException() {
    super(409);
  }
}
