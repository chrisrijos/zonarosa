/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.time.Instant;

public class InstantAdapter {

  public static class EpochSecondSerializer extends JsonSerializer<Instant> {

    @Override
    public void serialize(final Instant value, final JsonGenerator gen, final SerializerProvider serializers)
        throws IOException {

      gen.writeNumber(value.getEpochSecond());
    }
  }

}
