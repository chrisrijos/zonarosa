/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.util.concurrent;

public final class FutureTransformers {

  public static <Input, Output> ListenableFuture<Output> map(ListenableFuture<Input> future, Transformer<Input, Output> transformer) {
    return new FutureMapTransformer<>(future, transformer);
  }

  public interface Transformer<Input, Output> {
    Output transform(Input a) throws Exception;
  }
}
