//
// Copyright 2014-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.state.impl;

import java.util.HashMap;
import java.util.Map;
import io.zonarosa.libzonarosa.protocol.IdentityKey;
import io.zonarosa.libzonarosa.protocol.IdentityKeyPair;
import io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress;
import io.zonarosa.libzonarosa.protocol.state.IdentityKeyStore;

public class InMemoryIdentityKeyStore implements IdentityKeyStore {

  private final Map<ZonaRosaProtocolAddress, IdentityKey> trustedKeys = new HashMap<>();

  private final IdentityKeyPair identityKeyPair;
  private final int localRegistrationId;

  public InMemoryIdentityKeyStore(IdentityKeyPair identityKeyPair, int localRegistrationId) {
    this.identityKeyPair = identityKeyPair;
    this.localRegistrationId = localRegistrationId;
  }

  @Override
  public IdentityKeyPair getIdentityKeyPair() {
    return identityKeyPair;
  }

  @Override
  public int getLocalRegistrationId() {
    return localRegistrationId;
  }

  @Override
  public IdentityChange saveIdentity(ZonaRosaProtocolAddress address, IdentityKey identityKey) {
    IdentityKey existing = trustedKeys.get(address);
    trustedKeys.put(address, identityKey);

    if (existing == null || identityKey.equals(existing)) {
      return IdentityChange.NEW_OR_UNCHANGED;
    } else {
      return IdentityChange.REPLACED_EXISTING;
    }
  }

  @Override
  public boolean isTrustedIdentity(
      ZonaRosaProtocolAddress address, IdentityKey identityKey, Direction direction) {
    IdentityKey trusted = trustedKeys.get(address);
    return (trusted == null || trusted.equals(identityKey));
  }

  @Override
  public IdentityKey getIdentity(ZonaRosaProtocolAddress address) {
    return trustedKeys.get(address);
  }
}
