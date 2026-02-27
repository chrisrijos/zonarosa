/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.auth;

import com.google.common.annotations.VisibleForTesting;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.eclipse.jetty.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.websocket.server.JettyServerUpgradeResponse;
import io.zonarosa.server.metrics.MetricsUtil;
import io.zonarosa.server.storage.Device;
import io.zonarosa.websocket.auth.AuthenticatedWebSocketUpgradeFilter;

public class IdlePrimaryDeviceAuthenticatedWebSocketUpgradeFilter implements
    AuthenticatedWebSocketUpgradeFilter<AuthenticatedDevice> {

  private final Duration minIdleDuration;
  private final Clock clock;

  @VisibleForTesting
  static final String ALERT_HEADER = "X-ZonaRosa-Alert";

  @VisibleForTesting
  static final String IDLE_PRIMARY_DEVICE_ALERT = "idle-primary-device";

  private static final Counter IDLE_PRIMARY_WARNING_COUNTER = Metrics.counter(
      MetricsUtil.name(IdlePrimaryDeviceAuthenticatedWebSocketUpgradeFilter.class, "idlePrimaryDeviceWarning"),
      "critical", "false");

  public IdlePrimaryDeviceAuthenticatedWebSocketUpgradeFilter(final Duration minIdleDuration, final Clock clock) {
    this.minIdleDuration = minIdleDuration;
    this.clock = clock;
  }

  @Override
  public void handleAuthentication(final Optional<AuthenticatedDevice> authenticated,
      final JettyServerUpgradeRequest request,
      final JettyServerUpgradeResponse response) {

    // No action needed if the connection is unauthenticated (in which case we don't know when we've last seen the
    // primary device) or if the authenticated device IS the primary device
    authenticated
        .filter(authenticatedDevice -> authenticatedDevice.deviceId() != Device.PRIMARY_ID)
        .ifPresent(authenticatedDevice -> {
          if (authenticatedDevice.primaryDeviceLastSeen().isBefore(clock.instant().minus(minIdleDuration))) {
            response.addHeader(ALERT_HEADER, IDLE_PRIMARY_DEVICE_ALERT);
            IDLE_PRIMARY_WARNING_COUNTER.increment();
          }
        });
  }
}
