package io.zonarosa.service.internal.push.exceptions;

import io.zonarosa.service.api.push.exceptions.NonSuccessfulResponseCodeException;

import java.util.function.Function;

import okhttp3.ResponseBody;

public final class PaymentsRegionException extends NonSuccessfulResponseCodeException {
  public PaymentsRegionException(int code) {
    super(code);
  }
}
