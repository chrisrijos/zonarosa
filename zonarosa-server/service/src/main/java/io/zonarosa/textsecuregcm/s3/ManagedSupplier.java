/*
 * Copyright 2013 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.s3;

import io.dropwizard.lifecycle.Managed;
import java.util.function.Supplier;

public interface ManagedSupplier<T> extends Supplier<T>, Managed {

  @Override
  default void start() throws Exception {
    // noop
  }

  @Override
  default void stop() throws Exception {
    // noop
  }
}
