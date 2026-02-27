/*
 * Copyright 2013-2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.asn;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nonnull;
import org.apache.commons.lang3.Validate;

public record AsnRange<T extends Comparable<T>>(@Nonnull T from,
                                                @Nonnull T to,
                                                @Nonnull AsnInfo asnInfo) {
  public AsnRange {
    requireNonNull(from);
    requireNonNull(to);
    requireNonNull(asnInfo);
    Validate.isTrue(from.compareTo(to) <= 0);
  }

  boolean contains(@Nonnull final T element) {
    requireNonNull(element);
    return from.compareTo(element) <= 0
        && element.compareTo(to) <= 0;
  }
}
