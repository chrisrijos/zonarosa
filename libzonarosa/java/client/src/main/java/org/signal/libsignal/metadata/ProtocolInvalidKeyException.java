//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.metadata;

import io.zonarosa.libzonarosa.metadata.protocol.UnidentifiedSenderMessageContent;
import io.zonarosa.libzonarosa.protocol.InvalidKeyException;

public class ProtocolInvalidKeyException extends ProtocolException {
  public ProtocolInvalidKeyException(InvalidKeyException e, String sender, int senderDevice) {
    super(e, sender, senderDevice);
  }

  ProtocolInvalidKeyException(InvalidKeyException e, UnidentifiedSenderMessageContent content) {
    super(e, content);
  }
}
