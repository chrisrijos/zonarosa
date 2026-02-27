//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import io.zonarosa.libzonarosa.internal.CompletableFuture;
import io.zonarosa.libzonarosa.internal.NativeTesting;
import io.zonarosa.libzonarosa.util.TestEnvironment;
import io.zonarosa.libzonarosa.util.TestLogger;

public class ChatServiceConnectTests {
  private static final String USER_AGENT = "test";

  private static class Listener implements ChatConnectionListener {
    CompletableFuture<ChatServiceException> disconnectReason = new CompletableFuture<>();

    public void onConnectionInterrupted(
        ChatConnection chat, ChatServiceException disconnectReason) {
      this.disconnectReason.complete(disconnectReason);
    }

    public void onIncomingMessage(
        ChatConnection chat,
        byte[] envelope,
        long serverDeliveryTimestamp,
        ChatConnectionListener.ServerMessageAck sendAck) {
      throw new AssertionError("Unexpected incoming message");
    }
  }

  static final String getProxyServer() {
    final String proxyServer = TestEnvironment.get("LIBZONAROSA_TESTING_PROXY_SERVER");
    Assume.assumeNotNull(proxyServer);
    // Allows to effectively "unset" the environment variable
    Assume.assumeFalse(proxyServer.isEmpty());
    return proxyServer;
  }

  @ClassRule public static final TestLogger logger = new TestLogger();

  @Rule public Timeout perCaseTimeout = new Timeout(15, TimeUnit.SECONDS);

  @Test
  public void testConnectUnauth() throws Exception {
    // Use the presence of the environment setting to know whether we should
    // make network requests in our tests.
    final String ENABLE_TEST = TestEnvironment.get("LIBZONAROSA_TESTING_RUN_NONHERMETIC_TESTS");
    Assume.assumeNotNull(ENABLE_TEST);

    final Network net = new Network(Network.Environment.STAGING, USER_AGENT);
    final Listener listener = new Listener();
    var chat = net.connectUnauthChat(listener).get();
    chat.start();
    Void disconnectFinished = chat.disconnect().get();

    ChatServiceException disconnectReason = listener.disconnectReason.get();
    assertNull(disconnectReason);
  }

  @Test
  public void testConnectUnauthH2() throws Exception {
    // Use the presence of the environment setting to know whether we should
    // make network requests in our tests.
    final String ENABLE_TEST = TestEnvironment.get("LIBZONAROSA_TESTING_RUN_NONHERMETIC_TESTS");
    Assume.assumeNotNull(ENABLE_TEST);

    final Network net = new Network(Network.Environment.STAGING, USER_AGENT);
    net.setRemoteConfig(Map.of("useH2ForUnauthChat", "true"), Network.BuildVariant.BETA);
    final Listener listener = new Listener();
    var chat = net.connectUnauthChat(listener).get();
    chat.start();
    Void disconnectFinished = chat.disconnect().get();

    ChatServiceException disconnectReason = listener.disconnectReason.get();
    assertNull(disconnectReason);
  }

  @Test
  public void testConnectCancellationUnauth() throws Exception {
    // Use the presence of the environment setting to know whether we should
    // make network requests in our tests.
    final String ENABLE_TEST = TestEnvironment.get("LIBZONAROSA_TESTING_RUN_NONHERMETIC_TESTS");
    Assume.assumeNotNull(ENABLE_TEST);

    final Network net = new Network(Network.Environment.STAGING, USER_AGENT);
    final Listener listener = new Listener();
    final CompletableFuture<UnauthenticatedChatConnection> connectFuture =
        net.connectUnauthChat(listener);
    assertTrue("Expected cancellation of connect in progress", connectFuture.cancel(true));
    assertThrows(CancellationException.class, () -> connectFuture.get());
  }

  @Test
  public void testPreconnectAuth() throws Exception {
    // Use the presence of the environment setting to know whether we should
    // make network requests in our tests.
    final String ENABLE_TEST = TestEnvironment.get("LIBZONAROSA_TESTING_RUN_NONHERMETIC_TESTS");
    Assume.assumeNotNull(ENABLE_TEST);

    final Network net = new Network(Network.Environment.STAGING, USER_AGENT);
    final Listener listener = new Listener();
    net.preconnectChat().get();

    // While we get no direct feedback here whether the preconnect was used,
    // you can check the log lines for: "[authenticated] using preconnection".
    // We have to use an authenticated connection because that's the only one that's allowed to
    // use preconnects.
    final var e =
        assertThrows(
            ExecutionException.class, () -> net.connectAuthChat("", "", false, listener).get());
    assertTrue(e.getCause() instanceof DeviceDeregisteredException);
  }

  @Test
  public void testConnectUnauthThroughProxy() throws Exception {
    final String PROXY_SERVER = getProxyServer();

    final Network net = new Network(Network.Environment.STAGING, USER_AGENT);
    final String[] proxyComponents = PROXY_SERVER.split(":");
    switch (proxyComponents.length) {
      case 1:
        net.setProxy(PROXY_SERVER, 443);
        break;
      case 2:
        net.setProxy(proxyComponents[0], Integer.parseInt(proxyComponents[1]));
        break;
      default:
        throw new IllegalArgumentException("invalid LIBZONAROSA_TESTING_PROXY_SERVER");
    }
    assertEquals(
        (int)
            net.getConnectionManager()
                .guardedMap(NativeTesting::TESTING_ConnectionManager_isUsingProxy),
        1);

    final Listener listener = new Listener();
    var chat = net.connectUnauthChat(listener).get();
    chat.start();
    Void disconnectFinished = chat.disconnect().get();

    ChatServiceException disconnectReason = listener.disconnectReason.get();
    assertNull(disconnectReason);
  }

  @Test
  public void testConnectUnauthThroughProxyByParts() throws Exception {
    final String PROXY_SERVER = getProxyServer();

    final Network net = new Network(Network.Environment.STAGING, USER_AGENT);

    String host;
    Integer port;
    final String[] proxyComponents = PROXY_SERVER.split(":");
    switch (proxyComponents.length) {
      case 1:
        host = PROXY_SERVER;
        port = null;
        break;
      case 2:
        host = proxyComponents[0];
        port = Integer.parseInt(proxyComponents[1]);
        break;
      default:
        throw new IllegalArgumentException("invalid LIBZONAROSA_TESTING_PROXY_SERVER");
    }

    String username;
    final String[] hostComponents = host.split("@");
    switch (hostComponents.length) {
      case 1:
        username = null;
        break;
      case 2:
        username = hostComponents[0];
        host = hostComponents[1];
        break;
      default:
        throw new IllegalArgumentException("invalid LIBZONAROSA_TESTING_PROXY_SERVER");
    }

    net.setProxy(Network.ZONAROSA_TLS_PROXY_SCHEME, host, port, username, null);
    assertEquals(
        (int)
            net.getConnectionManager()
                .guardedMap(NativeTesting::TESTING_ConnectionManager_isUsingProxy),
        1);

    final Listener listener = new Listener();
    var chat = net.connectUnauthChat(listener).get();
    chat.start();
    Void disconnectFinished = chat.disconnect().get();

    ChatServiceException disconnectReason = listener.disconnectReason.get();
    assertNull(disconnectReason);
  }
}
