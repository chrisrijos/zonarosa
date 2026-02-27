/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.zonarosa.server.identity.ServiceIdentifier;
import io.zonarosa.server.identity.AciServiceIdentifier;
import io.zonarosa.server.identity.PniServiceIdentifier;

public class ServiceIdentifierAdapter {

  public static class ServiceIdentifierSerializer extends JsonSerializer<ServiceIdentifier> {

    @Override
    public void serialize(final ServiceIdentifier identifier, final JsonGenerator jsonGenerator, final SerializerProvider serializers)
        throws IOException {

      jsonGenerator.writeString(identifier.toServiceIdentifierString());
    }
  }

  public static class AciServiceIdentifierDeserializer extends JsonDeserializer<AciServiceIdentifier> {

    @Override
    public AciServiceIdentifier deserialize(final JsonParser parser, final DeserializationContext context)
        throws IOException {

      return AciServiceIdentifier.valueOf(parser.getValueAsString());
    }
  }

  public static class PniServiceIdentifierDeserializer extends JsonDeserializer<PniServiceIdentifier> {

    @Override
    public PniServiceIdentifier deserialize(final JsonParser parser, final DeserializationContext context)
        throws IOException {

      return PniServiceIdentifier.valueOf(parser.getValueAsString());
    }
  }

  public static class ServiceIdentifierDeserializer extends JsonDeserializer<ServiceIdentifier> {

    @Override
    public ServiceIdentifier deserialize(final JsonParser parser, final DeserializationContext context)
        throws IOException {

      return ServiceIdentifier.valueOf(parser.getValueAsString());
    }
  }
}
