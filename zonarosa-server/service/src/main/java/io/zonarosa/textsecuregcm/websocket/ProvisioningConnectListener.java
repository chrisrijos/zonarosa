/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.websocket;

import com.google.common.annotations.VisibleForTesting;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import io.zonarosa.server.auth.AuthenticatedDevice;
import io.zonarosa.server.controllers.ProvisioningController;
import io.zonarosa.server.entities.MessageProtos;
import io.zonarosa.server.entities.ProvisioningMessage;
import io.zonarosa.server.metrics.OpenWebSocketCounter;
import io.zonarosa.server.push.ProvisioningManager;
import io.zonarosa.server.storage.ClientReleaseManager;
import io.zonarosa.server.storage.PubSubProtos;
import io.zonarosa.server.util.HeaderUtils;
import io.zonarosa.websocket.session.WebSocketSessionContext;
import io.zonarosa.websocket.setup.WebSocketConnectListener;

/**
 * A "provisioning WebSocket" provides a mechanism for sending a caller-defined provisioning message from the primary
 * device associated with a ZonaRosa account to a new device that is not yet associated with a ZonaRosa account. Generally,
 * the message contains key material and credentials the new device needs to associate itself with the primary device's
 * ZonaRosa account.
 * <p>
 * New devices initiate the provisioning process by opening a provisioning WebSocket. The server assigns the new device
 * a random, temporary "provisioning address," which it transmits via the newly-opened WebSocket. From there, the new
 * device generally displays the provisioning address (and a public key) as a QR code. After that, the primary device
 * will scan the QR code and send an encrypted provisioning message to the new device via
 * {@link ProvisioningController#sendProvisioningMessage(AuthenticatedDevice, String, ProvisioningMessage, String)}.
 * Once the server receives the message from the primary device, it sends the message to the new device via the open
 * WebSocket, then closes the WebSocket connection.
 */
public class ProvisioningConnectListener implements WebSocketConnectListener {

  private final ProvisioningManager provisioningManager;
  private final OpenWebSocketCounter openWebSocketCounter;
  private final ScheduledExecutorService timeoutExecutor;
  private final Duration timeout;

  public ProvisioningConnectListener(final ProvisioningManager provisioningManager,
      final ClientReleaseManager clientReleaseManager,
      final ScheduledExecutorService timeoutExecutor,
      final Duration timeout) {
    this.provisioningManager = provisioningManager;
    this.timeoutExecutor = timeoutExecutor;
    this.timeout = timeout;
    this.openWebSocketCounter = new OpenWebSocketCounter("provisioning", clientReleaseManager);
  }

  @Override
  public void onWebSocketConnect(WebSocketSessionContext context) {
    openWebSocketCounter.countOpenWebSocket(context);

    final ScheduledFuture<?> timeoutFuture = timeoutExecutor.schedule(() ->
            context.getClient().close(1000, "Timeout"), timeout.toSeconds(), TimeUnit.SECONDS);

    final String provisioningAddress = generateProvisioningAddress();

    context.addWebsocketClosedListener((_, _, _) -> {
      provisioningManager.removeListener(provisioningAddress);
      timeoutFuture.cancel(false);
    });

    provisioningManager.addListener(provisioningAddress, message -> {
      assert message.getType() == PubSubProtos.PubSubMessage.Type.DELIVER;

      final Optional<byte[]> body = Optional.of(message.getContent().toByteArray());

      context.getClient().sendRequest("PUT", "/v1/message", List.of(HeaderUtils.getTimestampHeader()), body)
          .whenComplete((_, _) -> context.getClient().close(1000, "Closed"));
    });

    context.getClient().sendRequest("PUT", "/v1/address", List.of(HeaderUtils.getTimestampHeader()),
        Optional.of(MessageProtos.ProvisioningAddress.newBuilder()
            .setAddress(provisioningAddress)
            .build().toByteArray()));
  }

  @VisibleForTesting
  public static String generateProvisioningAddress() {
    final byte[] provisioningAddress = new byte[16];
    new SecureRandom().nextBytes(provisioningAddress);

    return Base64.getUrlEncoder().encodeToString(provisioningAddress);
  }
}
