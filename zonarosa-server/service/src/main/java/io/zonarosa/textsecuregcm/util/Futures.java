/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.util;

import java.util.concurrent.CompletionStage;
import org.apache.commons.lang3.function.TriFunction;

public class Futures {

  public static <T, U, V, R> CompletionStage<R> zipWith(
      CompletionStage<T> futureT,
      CompletionStage<U> futureU,
      CompletionStage<V> futureV,
      TriFunction<T, U, V, R> fun) {

    return futureT.thenCompose(t -> futureU.thenCombine(futureV, (u, v) -> fun.apply(t, u, v)));
  }
}
