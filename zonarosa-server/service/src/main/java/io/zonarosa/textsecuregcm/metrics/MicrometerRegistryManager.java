/*
 * Copyright 2022 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import org.eclipse.jetty.util.component.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MicrometerRegistryManager implements LifeCycle.Listener {

  private static final Logger logger = LoggerFactory.getLogger(MicrometerRegistryManager.class);

  private final MeterRegistry meterRegistry;
  private final Duration waitDuration;

  public MicrometerRegistryManager(final MeterRegistry meterRegistry, final Duration waitDuration) {
    this.meterRegistry = meterRegistry;
    this.waitDuration = waitDuration;
  }

  @Override
  public void lifeCycleStopped(final LifeCycle event) {
    try {
      logger.info("Waiting for {} to ensure final metrics are polled and published", waitDuration);
      Thread.sleep(waitDuration.toMillis());
    } catch (final InterruptedException e) {
      logger.warn("Waiting interrupted", e);
    } finally {
      meterRegistry.close();
    }
  }

}
