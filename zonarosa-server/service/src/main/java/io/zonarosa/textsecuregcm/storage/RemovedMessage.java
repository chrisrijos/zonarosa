/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.storage;

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import java.util.UUID;
import io.zonarosa.server.entities.MessageProtos;
import io.zonarosa.server.identity.ServiceIdentifier;

public record RemovedMessage(Optional<ServiceIdentifier> sourceServiceId, ServiceIdentifier destinationServiceId,
                             @VisibleForTesting UUID serverGuid, long serverTimestamp, long clientTimestamp,
                             MessageProtos.Envelope.Type envelopeType) {

  public static RemovedMessage fromEnvelope(MessageProtos.Envelope envelope) {
    return new RemovedMessage(
        envelope.hasSourceServiceId()
            ? Optional.of(ServiceIdentifier.valueOf(envelope.getSourceServiceId()))
            : Optional.empty(),
        ServiceIdentifier.valueOf(envelope.getDestinationServiceId()),
        UUID.fromString(envelope.getServerGuid()),
        envelope.getServerTimestamp(),
        envelope.getClientTimestamp(),
        envelope.getType()
    );
  }
}
