/*
 * Copyright 2013-2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.asn;

import java.util.Optional;
import javax.annotation.Nonnull;

public interface AsnInfoProvider {

  /// Gets ASN information for an IP address.
  ///
  /// @param ipString a string representation of an IP address
  ///
  /// @return ASN information for the given IP address or empty if no ASN information was found for the given IP address
  Optional<AsnInfo> lookup(@Nonnull String ipString);

  AsnInfoProvider EMPTY = _ -> Optional.empty();
}
