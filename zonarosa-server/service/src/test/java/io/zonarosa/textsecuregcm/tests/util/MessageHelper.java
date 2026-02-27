/*
 * Copyright 2022 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.tests.util;

import com.google.protobuf.ByteString;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import io.zonarosa.server.entities.MessageProtos;

public class MessageHelper {

  public static MessageProtos.Envelope createMessage(UUID senderUuid, final byte senderDeviceId, UUID destinationUuid,
      long timestamp, String content) {
    return MessageProtos.Envelope.newBuilder()
        .setServerGuid(UUID.randomUUID().toString())
        .setType(MessageProtos.Envelope.Type.CIPHERTEXT)
        .setClientTimestamp(timestamp)
        .setServerTimestamp(0)
        .setSourceServiceId(senderUuid.toString())
        .setSourceDevice(senderDeviceId)
        .setDestinationServiceId(destinationUuid.toString())
        .setContent(ByteString.copyFrom(content.getBytes(StandardCharsets.UTF_8)))
        .build();
  }
}
