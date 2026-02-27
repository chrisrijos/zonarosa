/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.util;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

public class ExecutorUtil {

  private ExecutorUtil() {
  }

  /**
   * Submit all runnables to executorService and wait for them all to complete.
   * <p>
   * If any runnable completes exceptionally, after all runnables have completed the first exception will be thrown
   *
   * @param executor  The executor to run runnables
   * @param runnables A collection of runnables to run
   */
  public static void runAll(Executor executor, Collection<Runnable> runnables) {
    try {
      CompletableFuture.allOf(runnables
              .stream()
              .map(runnable -> CompletableFuture.runAsync(runnable, executor))
              .toArray(CompletableFuture[]::new))
          .join();
    } catch (CompletionException e) {
      final Throwable cause = e.getCause();
      // These exceptions should always be RuntimeExceptions because Runnable does not throw
      if (cause instanceof RuntimeException re) {
        throw re;
      } else {
        throw new IllegalStateException(cause);
      }
    }
  }
}
