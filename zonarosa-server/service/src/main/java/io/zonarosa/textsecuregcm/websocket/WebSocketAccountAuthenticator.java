/*
 * Copyright 2013 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.websocket;

import static io.zonarosa.server.util.HeaderUtils.basicCredentialsFromAuthHeader;

import com.google.common.net.HttpHeaders;
import javax.annotation.Nullable;
import io.dropwizard.auth.basic.BasicCredentials;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import io.zonarosa.server.auth.AccountAuthenticator;
import io.zonarosa.server.auth.AuthenticatedDevice;
import io.zonarosa.websocket.auth.InvalidCredentialsException;
import io.zonarosa.websocket.auth.WebSocketAuthenticator;
import java.util.Optional;


public class WebSocketAccountAuthenticator implements WebSocketAuthenticator<AuthenticatedDevice> {

  private final AccountAuthenticator accountAuthenticator;

  public WebSocketAccountAuthenticator(final AccountAuthenticator accountAuthenticator) {
    this.accountAuthenticator = accountAuthenticator;
  }

  @Override
  public Optional<AuthenticatedDevice> authenticate(final UpgradeRequest request)
      throws InvalidCredentialsException {

    @Nullable final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (authHeader == null) {
      return Optional.empty();
    }

    final BasicCredentials credentials = basicCredentialsFromAuthHeader(authHeader)
        .orElseThrow(InvalidCredentialsException::new);

    final AuthenticatedDevice authenticatedDevice = accountAuthenticator.authenticate(credentials)
        .orElseThrow(InvalidCredentialsException::new);

    return Optional.of(authenticatedDevice);
  }
}
