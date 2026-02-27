package io.zonarosa.server.storage;

import io.zonarosa.server.util.NoStackTraceRuntimeException;

class MrmDataMissingException extends NoStackTraceRuntimeException {

  enum Type {
    SHARED,
    RECIPIENT_VIEW
  }

  private final Type type;

  MrmDataMissingException(final Type type) {
    this.type = type;
  }

  Type getType() {
    return type;
  }

  @Override
  public String toString() {
    return "MrmDataMissingException{type=%s}".formatted(type);
  }
}
