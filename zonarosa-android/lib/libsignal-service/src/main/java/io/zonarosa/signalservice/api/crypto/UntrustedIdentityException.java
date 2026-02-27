/**
 * Copyright (C) 2014-2016 ZonaRosa Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package io.zonarosa.service.api.crypto;

import io.zonarosa.libzonarosa.protocol.IdentityKey;

public class UntrustedIdentityException extends Exception {

  private final IdentityKey identityKey;
  private final String      identifier;

  public UntrustedIdentityException(String s, String identifier, IdentityKey identityKey) {
    super(s);
    this.identifier  = identifier;
    this.identityKey = identityKey;
  }

  public UntrustedIdentityException(UntrustedIdentityException e) {
    this(e.getMessage(), e.getIdentifier(), e.getIdentityKey());
  }

  public IdentityKey getIdentityKey() {
    return identityKey;
  }

  public String getIdentifier() {
    return identifier;
  }

}
