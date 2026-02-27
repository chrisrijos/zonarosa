/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.util;

import static io.zonarosa.server.metrics.MetricsUtil.name;

import io.dropwizard.core.setup.Environment;
import io.dropwizard.lifecycle.ExecutorServiceManager;
import io.dropwizard.util.Duration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Build Executor Services managed by dropwizard, supplementing executors provided by
 * {@link io.dropwizard.lifecycle.setup.LifecycleEnvironment#executorService}
 */
public class ManagedExecutors {

  private static final Duration SHUTDOWN_DURATION = Duration.seconds(5);

  private ManagedExecutors() {
  }

  public static ExecutorService newVirtualThreadPerTaskExecutor(
      final String threadNamePrefix,
      final int maxConcurrentThreads,
      final Environment environment) {

    final BoundedVirtualThreadFactory threadFactory =
        new BoundedVirtualThreadFactory(threadNamePrefix, maxConcurrentThreads);
    final ExecutorService virtualThreadExecutor = Executors.newThreadPerTaskExecutor(threadFactory);
    environment.lifecycle()
        .manage(new ExecutorServiceManager(virtualThreadExecutor, SHUTDOWN_DURATION, threadNamePrefix));
    return virtualThreadExecutor;
  }
}
