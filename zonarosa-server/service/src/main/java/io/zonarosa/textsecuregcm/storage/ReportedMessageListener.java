/*
 * Copyright 2013-2022 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.storage;

import java.util.Optional;
import java.util.UUID;

public interface ReportedMessageListener {

  void handleMessageReported(String sourceNumber, UUID messageGuid, UUID reporterUuid, Optional<byte[]> reportSpamToken);
}
