/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.storage;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

/**
 * The built-in {@link com.fasterxml.jackson.databind.deser.std.NumberDeserializers.ByteDeserializer} will return
 * negative values&mdash;both verbatim and by coercing 128&hellip;255. We prefer this invalid data to fail fast, so this
 * is a simpler and stricter deserializer.
 */
public class DeviceIdDeserializer extends JsonDeserializer<Byte> {

  @Override
  public Byte deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

    byte value = p.getByteValue();

    if (value < Device.PRIMARY_ID) {
      throw new DeviceIdDeserializationException();
    }

    return value;
  }

  static class DeviceIdDeserializationException extends IOException {

    DeviceIdDeserializationException() {
      super("Invalid Device ID");
    }

  }


}
