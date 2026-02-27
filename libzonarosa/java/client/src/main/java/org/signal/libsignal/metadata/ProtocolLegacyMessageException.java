//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.metadata;

import io.zonarosa.libzonarosa.metadata.protocol.UnidentifiedSenderMessageContent;
import io.zonarosa.libzonarosa.protocol.LegacyMessageException;

public class ProtocolLegacyMessageException extends ProtocolException {
  public ProtocolLegacyMessageException(
      LegacyMessageException e, String sender, int senderDeviceId) {
    super(e, sender, senderDeviceId);
  }

  ProtocolLegacyMessageException(
      LegacyMessageException e, UnidentifiedSenderMessageContent content) {
    super(e, content);
  }
}
