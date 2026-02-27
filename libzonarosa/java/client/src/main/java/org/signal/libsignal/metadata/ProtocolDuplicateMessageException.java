//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.metadata;

import io.zonarosa.libzonarosa.metadata.protocol.UnidentifiedSenderMessageContent;

public class ProtocolDuplicateMessageException extends ProtocolException {
  public ProtocolDuplicateMessageException(Exception e, String sender, int senderDevice) {
    super(e, sender, senderDevice);
  }

  ProtocolDuplicateMessageException(Exception e, UnidentifiedSenderMessageContent content) {
    super(e, content);
  }
}
