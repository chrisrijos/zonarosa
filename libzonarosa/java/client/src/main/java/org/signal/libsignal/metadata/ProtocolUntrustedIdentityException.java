//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.metadata;

import io.zonarosa.libzonarosa.metadata.protocol.UnidentifiedSenderMessageContent;
import io.zonarosa.libzonarosa.protocol.UntrustedIdentityException;

public class ProtocolUntrustedIdentityException extends ProtocolException {
  public ProtocolUntrustedIdentityException(
      UntrustedIdentityException e, String sender, int senderDevice) {
    super(e, sender, senderDevice);
  }

  ProtocolUntrustedIdentityException(
      UntrustedIdentityException e, UnidentifiedSenderMessageContent content) {
    super(e, content);
  }
}
