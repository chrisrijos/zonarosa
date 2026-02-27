//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.metadata;

import io.zonarosa.libzonarosa.metadata.protocol.UnidentifiedSenderMessageContent;
import io.zonarosa.libzonarosa.protocol.NoSessionException;

public class ProtocolNoSessionException extends ProtocolException {
  public ProtocolNoSessionException(NoSessionException e, String sender, int senderDevice) {
    super(e, sender, senderDevice);
  }

  ProtocolNoSessionException(NoSessionException e, UnidentifiedSenderMessageContent content) {
    super(e, content);
  }
}
