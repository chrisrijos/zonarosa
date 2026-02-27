/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.zonarosa.server.auth.AuthenticatedDevice;
import io.zonarosa.server.auth.DisconnectionRequestManager;
import io.zonarosa.server.identity.IdentityType;
import io.zonarosa.server.storage.Account;
import io.zonarosa.server.storage.AccountsManager;
import io.zonarosa.server.storage.ClientReleaseManager;
import io.zonarosa.server.storage.Device;
import io.zonarosa.websocket.WebSocketClient;
import io.zonarosa.websocket.session.WebSocketSessionContext;

class AuthenticatedConnectListenerTest {

  private AccountsManager accountsManager;
  private DisconnectionRequestManager disconnectionRequestManager;

  private WebSocketConnection authenticatedWebSocketConnection;
  private AuthenticatedConnectListener authenticatedConnectListener;

  private Account authenticatedAccount;
  private WebSocketClient webSocketClient;
  private WebSocketSessionContext webSocketSessionContext;

  private static final UUID ACCOUNT_IDENTIFIER = UUID.randomUUID();
  private static final byte DEVICE_ID = Device.PRIMARY_ID;

  @BeforeEach
  void setUpBeforeEach() {
    accountsManager = mock(AccountsManager.class);
    disconnectionRequestManager = mock(DisconnectionRequestManager.class);

    authenticatedWebSocketConnection = mock(WebSocketConnection.class);

    authenticatedConnectListener = new AuthenticatedConnectListener(accountsManager,
        disconnectionRequestManager,
        mock(ClientReleaseManager.class),
        (_, _, _) -> authenticatedWebSocketConnection);

    final Device device = mock(Device.class);
    when(device.getId()).thenReturn(DEVICE_ID);

    authenticatedAccount = mock(Account.class);
    when(authenticatedAccount.getIdentifier(IdentityType.ACI)).thenReturn(ACCOUNT_IDENTIFIER);
    when(authenticatedAccount.getDevice(DEVICE_ID)).thenReturn(Optional.of(device));

    webSocketClient = mock(WebSocketClient.class);

    webSocketSessionContext = mock(WebSocketSessionContext.class);
    when(webSocketSessionContext.getClient()).thenReturn(webSocketClient);
  }

  @Test
  void onWebSocketConnectAuthenticated() {
    when(webSocketSessionContext.getAuthenticated()).thenReturn(new AuthenticatedDevice(ACCOUNT_IDENTIFIER, DEVICE_ID, Instant.now()));
    when(webSocketSessionContext.getAuthenticated(AuthenticatedDevice.class))
        .thenReturn(new AuthenticatedDevice(ACCOUNT_IDENTIFIER, DEVICE_ID, Instant.now()));

    when(accountsManager.getByAccountIdentifier(ACCOUNT_IDENTIFIER)).thenReturn(Optional.of(authenticatedAccount));

    authenticatedConnectListener.onWebSocketConnect(webSocketSessionContext);

    verify(disconnectionRequestManager).addListener(ACCOUNT_IDENTIFIER, DEVICE_ID, authenticatedWebSocketConnection);
    // We expect one call from AuthenticatedConnectListener itself and one from OpenWebSocketCounter
    verify(webSocketSessionContext, times(2)).addWebsocketClosedListener(any());
    verify(authenticatedWebSocketConnection).start();
  }

  @Test
  void onWebSocketConnectAuthenticatedAccountNotFound() {
    when(webSocketSessionContext.getAuthenticated()).thenReturn(new AuthenticatedDevice(ACCOUNT_IDENTIFIER, DEVICE_ID, Instant.now()));
    when(webSocketSessionContext.getAuthenticated(AuthenticatedDevice.class))
        .thenReturn(new AuthenticatedDevice(ACCOUNT_IDENTIFIER, DEVICE_ID, Instant.now()));

    when(accountsManager.getByAccountIdentifier(ACCOUNT_IDENTIFIER)).thenReturn(Optional.empty());

    authenticatedConnectListener.onWebSocketConnect(webSocketSessionContext);

    verify(webSocketClient).close(eq(1011), anyString());

    verify(disconnectionRequestManager, never()).addListener(any(), anyByte(), any());
    // We expect one call from OpenWebSocketCounter, but none from AuthenticatedConnectListener itself
    verify(webSocketSessionContext, times(1)).addWebsocketClosedListener(any());
    verify(authenticatedWebSocketConnection, never()).start();
  }

  @Test
  void onWebSocketConnectAuthenticatedStartException() {
    when(webSocketSessionContext.getAuthenticated()).thenReturn(new AuthenticatedDevice(ACCOUNT_IDENTIFIER, DEVICE_ID, Instant.now()));
    when(webSocketSessionContext.getAuthenticated(AuthenticatedDevice.class))
        .thenReturn(new AuthenticatedDevice(ACCOUNT_IDENTIFIER, DEVICE_ID, Instant.now()));

    when(accountsManager.getByAccountIdentifier(ACCOUNT_IDENTIFIER)).thenReturn(Optional.of(authenticatedAccount));
    doThrow(new RuntimeException()).when(authenticatedWebSocketConnection).start();

    authenticatedConnectListener.onWebSocketConnect(webSocketSessionContext);

    verify(disconnectionRequestManager).addListener(ACCOUNT_IDENTIFIER, DEVICE_ID, authenticatedWebSocketConnection);
    // We expect one call from AuthenticatedConnectListener itself and one from OpenWebSocketCounter
    verify(webSocketSessionContext, times(2)).addWebsocketClosedListener(any());
    verify(authenticatedWebSocketConnection).start();

    verify(webSocketClient).close(eq(1011), anyString());
  }

  @Test
  void onWebSocketConnectUnauthenticated() {
    authenticatedConnectListener.onWebSocketConnect(webSocketSessionContext);

    verify(disconnectionRequestManager, never()).addListener(any(), anyByte(), any());
    // We expect one call from OpenWebSocketCounter, but none from AuthenticatedConnectListener itself
    verify(webSocketSessionContext, times(1)).addWebsocketClosedListener(any());
    verify(authenticatedWebSocketConnection, never()).start();
  }
}
