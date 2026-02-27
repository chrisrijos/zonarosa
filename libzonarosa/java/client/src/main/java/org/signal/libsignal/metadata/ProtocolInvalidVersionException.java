//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.metadata;

import io.zonarosa.libzonarosa.metadata.protocol.UnidentifiedSenderMessageContent;
import io.zonarosa.libzonarosa.protocol.InvalidVersionException;

public class ProtocolInvalidVersionException extends ProtocolException {
  public ProtocolInvalidVersionException(
      InvalidVersionException e, String sender, int senderDevice) {
    super(e, sender, senderDevice);
  }

  ProtocolInvalidVersionException(
      InvalidVersionException e, UnidentifiedSenderMessageContent content) {
    super(e, content);
  }
}
