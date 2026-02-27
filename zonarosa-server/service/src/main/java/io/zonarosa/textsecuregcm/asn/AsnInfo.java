/*
 * Copyright 2013-2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.asn;

import static java.util.Objects.requireNonNull;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import javax.annotation.Nonnull;

public record AsnInfo(long asn, @Nonnull String regionCode) {

  public AsnInfo {
    requireNonNull(regionCode, "regionCode must not be null");
  }
}
