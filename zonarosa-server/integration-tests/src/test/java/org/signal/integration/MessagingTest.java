/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.integration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import io.zonarosa.server.entities.IncomingMessage;
import io.zonarosa.server.entities.IncomingMessageList;
import io.zonarosa.server.entities.OutgoingMessageEntityList;
import io.zonarosa.server.entities.SendMessageResponse;
import io.zonarosa.server.storage.Device;

public class MessagingTest {

  @Test
  public void testSendMessageUnsealed() {
    final TestUser userA = Operations.newRegisteredUser("+19995550102");
    final TestUser userB = Operations.newRegisteredUser("+19995550103");

    try {
      final byte[] expectedContent = "Hello, World!".getBytes(StandardCharsets.UTF_8);
      final IncomingMessage message = new IncomingMessage(1, Device.PRIMARY_ID, userB.registrationId(), expectedContent);
      final IncomingMessageList messages = new IncomingMessageList(List.of(message), false, true, System.currentTimeMillis());

      Operations
          .apiPut("/v1/messages/%s".formatted(userB.aciUuid().toString()), messages)
          .authorized(userA)
          .execute(SendMessageResponse.class);

      final Pair<Integer, OutgoingMessageEntityList> receiveMessages = Operations.apiGet("/v1/messages")
          .authorized(userB)
          .execute(OutgoingMessageEntityList.class);

      final byte[] actualContent = receiveMessages.getRight().messages().getFirst().content();
      assertArrayEquals(expectedContent, actualContent);
    } finally {
      Operations.deleteUser(userA);
      Operations.deleteUser(userB);
    }
  }
}
