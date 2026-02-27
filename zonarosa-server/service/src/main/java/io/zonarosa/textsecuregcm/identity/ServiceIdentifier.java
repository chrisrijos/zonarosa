/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.identity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import io.zonarosa.libzonarosa.protocol.ServiceId;

/**
 * A "service identifier" is a tuple of a UUID and identity type that identifies an account and identity within the
 * ZonaRosa service.
 */
@Schema(
    type = "string",
    description = "A service identifier is a tuple of a UUID and identity type that identifies an account and identity within the ZonaRosa service.",
    subTypes = {AciServiceIdentifier.class, PniServiceIdentifier.class}
)
public sealed interface ServiceIdentifier permits AciServiceIdentifier, PniServiceIdentifier {

  /**
   * Returns the identity type of this account identifier.
   *
   * @return the identity type of this account identifier
   */
  IdentityType identityType();

  /**
   * Returns the UUID for this account identifier.
   *
   * @return the UUID for this account identifier
   */
  UUID uuid();

  /**
   * Returns a string representation of this account identifier in a format that clients can unambiguously resolve into
   * an identity type and UUID.
   *
   * @return a "strongly-typed" string representation of this account identifier
   */
  String toServiceIdentifierString();

  /**
   * Returns a compact binary representation of this account identifier.
   *
   * @return a binary representation of this account identifier
   */
  byte[] toCompactByteArray();

  /**
   * Returns a fixed-width binary representation of this account identifier.
   *
   * @return a binary representation of this account identifier
   */
  byte[] toFixedWidthByteArray();

  /**
   * Parse a service identifier string, which should be a plain UUID string for ACIs and a prefixed UUID string for PNIs
   *
   * @param string A service identifier string
   * @return The parsed {@link ServiceIdentifier}
   */
  static ServiceIdentifier valueOf(final String string) {
    try {
      return AciServiceIdentifier.valueOf(string);
    } catch (final IllegalArgumentException e) {
      return PniServiceIdentifier.valueOf(string);
    }
  }

  static ServiceIdentifier fromBytes(final byte[] bytes) {
    try {
      return AciServiceIdentifier.fromBytes(bytes);
    } catch (final IllegalArgumentException e) {
      return PniServiceIdentifier.fromBytes(bytes);
    }
  }

  static ServiceIdentifier fromLibzonarosa(final ServiceId libzonarosaServiceId) {
    if (libzonarosaServiceId instanceof ServiceId.Aci) {
      return new AciServiceIdentifier(libzonarosaServiceId.getRawUUID());
    }
    if (libzonarosaServiceId instanceof ServiceId.Pni) {
      return new PniServiceIdentifier(libzonarosaServiceId.getRawUUID());
    }
    throw new IllegalArgumentException("unknown libzonarosa ServiceId type");
  }

  ServiceId toLibzonarosa();
}
