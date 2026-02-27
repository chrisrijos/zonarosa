/*
 * Copyright 2013-2022 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.websocket;

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.zonarosa.server.auth.AuthenticatedDevice;
import io.zonarosa.server.auth.DisconnectionRequestManager;
import io.zonarosa.server.experiment.ExperimentEnrollmentManager;
import io.zonarosa.server.identity.IdentityType;
import io.zonarosa.server.limits.MessageDeliveryLoopMonitor;
import io.zonarosa.server.metrics.MessageMetrics;
import io.zonarosa.server.metrics.OpenWebSocketCounter;
import io.zonarosa.server.push.PushNotificationManager;
import io.zonarosa.server.push.PushNotificationScheduler;
import io.zonarosa.server.push.ReceiptSender;
import io.zonarosa.server.storage.Account;
import io.zonarosa.server.storage.AccountsManager;
import io.zonarosa.server.storage.ClientReleaseManager;
import io.zonarosa.server.storage.Device;
import io.zonarosa.server.storage.MessagesManager;
import io.zonarosa.websocket.WebSocketClient;
import io.zonarosa.websocket.session.WebSocketSessionContext;
import io.zonarosa.websocket.setup.WebSocketConnectListener;
import reactor.core.scheduler.Scheduler;

public class AuthenticatedConnectListener implements WebSocketConnectListener {

  private static final Logger log = LoggerFactory.getLogger(AuthenticatedConnectListener.class);

  private final AccountsManager accountsManager;
  private final DisconnectionRequestManager disconnectionRequestManager;
  private final WebSocketConnectionBuilder webSocketConnectionBuilder;

  private final OpenWebSocketCounter openAuthenticatedWebSocketCounter;
  private final OpenWebSocketCounter openUnauthenticatedWebSocketCounter;

  @VisibleForTesting
  @FunctionalInterface
  interface WebSocketConnectionBuilder {
    WebSocketConnection buildWebSocketConnection(Account account, Device device, WebSocketClient client);
  }

  public AuthenticatedConnectListener(
      final AccountsManager accountsManager,
      final ReceiptSender receiptSender,
      final MessagesManager messagesManager,
      final MessageMetrics messageMetrics,
      final PushNotificationManager pushNotificationManager,
      final PushNotificationScheduler pushNotificationScheduler,
      final DisconnectionRequestManager disconnectionRequestManager,
      final Scheduler messageDeliveryScheduler,
      final ClientReleaseManager clientReleaseManager,
      final MessageDeliveryLoopMonitor messageDeliveryLoopMonitor,
      final ExperimentEnrollmentManager experimentEnrollmentManager) {

    this(accountsManager,
        disconnectionRequestManager,
        clientReleaseManager,
        (account, device, client) -> new WebSocketConnection(receiptSender,
            messagesManager,
            messageMetrics,
            pushNotificationManager,
            pushNotificationScheduler,
            account,
            device,
            client,
            messageDeliveryScheduler,
            clientReleaseManager,
            messageDeliveryLoopMonitor,
            experimentEnrollmentManager)
    );
  }

  @VisibleForTesting AuthenticatedConnectListener(
      final AccountsManager accountsManager,
      final DisconnectionRequestManager disconnectionRequestManager,
      final ClientReleaseManager clientReleaseManager,
      final WebSocketConnectionBuilder webSocketConnectionBuilder) {

    this.accountsManager = accountsManager;
    this.disconnectionRequestManager = disconnectionRequestManager;
    this.webSocketConnectionBuilder = webSocketConnectionBuilder;

    this.openAuthenticatedWebSocketCounter = new OpenWebSocketCounter("rpc-authenticated", clientReleaseManager);
    this.openUnauthenticatedWebSocketCounter = new OpenWebSocketCounter("rpc-unauthenticated", clientReleaseManager);
  }

  @Override
  public void onWebSocketConnect(final WebSocketSessionContext context) {

    final boolean authenticated = (context.getAuthenticated() != null);

    (authenticated ? openAuthenticatedWebSocketCounter : openUnauthenticatedWebSocketCounter).countOpenWebSocket(context);

    if (authenticated) {
      final AuthenticatedDevice auth = context.getAuthenticated(AuthenticatedDevice.class);

      final Optional<Account> maybeAuthenticatedAccount =
          accountsManager.getByAccountIdentifier(auth.accountIdentifier());

      final Optional<Device> maybeAuthenticatedDevice =
          maybeAuthenticatedAccount.flatMap(account -> account.getDevice(auth.deviceId()));

      if (maybeAuthenticatedAccount.isEmpty() || maybeAuthenticatedDevice.isEmpty()) {
        log.warn("{}:{} not found when opening authenticated WebSocket", auth.accountIdentifier(), auth.deviceId());

        context.getClient().close(1011, "Unexpected error initializing connection");
        return;
      }

      final WebSocketConnection connection =
          webSocketConnectionBuilder.buildWebSocketConnection(maybeAuthenticatedAccount.get(),
              maybeAuthenticatedDevice.get(),
              context.getClient());

      disconnectionRequestManager.addListener(maybeAuthenticatedAccount.get().getIdentifier(IdentityType.ACI),
          maybeAuthenticatedDevice.get().getId(),
          connection);

      context.addWebsocketClosedListener((_, _, _) -> {
        disconnectionRequestManager.removeListener(maybeAuthenticatedAccount.get().getIdentifier(IdentityType.ACI),
            maybeAuthenticatedDevice.get().getId(),
            connection);

        connection.stop();
      });

      try {
        connection.start();
      } catch (final Exception e) {
        log.warn("Failed to initialize websocket", e);
        context.getClient().close(1011, "Unexpected error initializing connection");
      }
    }
  }
}
